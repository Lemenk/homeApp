package com.familyhome.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.familyhome.common.BizException;
import com.familyhome.dto.BillAccountItem;
import com.familyhome.dto.BillLogVO;
import com.familyhome.dto.BillQuery;
import com.familyhome.dto.BillRequest;
import com.familyhome.dto.BillVO;
import com.familyhome.dto.PageResult;
import com.familyhome.entity.Account;
import com.familyhome.entity.AuditLog;
import com.familyhome.entity.Bill;
import com.familyhome.entity.BillAccount;
import com.familyhome.entity.BillTag;
import com.familyhome.entity.Category;
import com.familyhome.entity.Ledger;
import com.familyhome.entity.Tag;
import com.familyhome.entity.User;
import com.familyhome.mapper.AccountMapper;
import com.familyhome.mapper.BillAccountMapper;
import com.familyhome.mapper.BillMapper;
import com.familyhome.mapper.BillTagMapper;
import com.familyhome.mapper.CategoryMapper;
import com.familyhome.mapper.LedgerMapper;
import com.familyhome.mapper.TagMapper;
import com.familyhome.security.UserContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class BillService {

    private final BillMapper billMapper;
    private final BillAccountMapper billAccountMapper;
    private final BillTagMapper billTagMapper;
    private final AccountMapper accountMapper;
    private final CategoryMapper categoryMapper;
    private final TagMapper tagMapper;
    private final LedgerMapper ledgerMapper;
    private final LedgerService ledgerService;
    private final UserService userService;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    public BillService(BillMapper billMapper, BillAccountMapper billAccountMapper,
                       BillTagMapper billTagMapper, AccountMapper accountMapper,
                       CategoryMapper categoryMapper, TagMapper tagMapper,
                       LedgerMapper ledgerMapper, LedgerService ledgerService,
                       UserService userService, AuditLogService auditLogService,
                       ObjectMapper objectMapper) {
        this.billMapper = billMapper;
        this.billAccountMapper = billAccountMapper;
        this.billTagMapper = billTagMapper;
        this.accountMapper = accountMapper;
        this.categoryMapper = categoryMapper;
        this.tagMapper = tagMapper;
        this.ledgerMapper = ledgerMapper;
        this.ledgerService = ledgerService;
        this.userService = userService;
        this.auditLogService = auditLogService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public BillVO createBill(Long userId, BillRequest req) {
        ledgerService.requireMember(req.getLedgerId(), userId);
        Ledger ledger = ledgerMapper.selectById(req.getLedgerId());
        if (ledger == null) {
            throw BizException.notFound("账本不存在");
        }
        BigDecimal amount = req.getAmount();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw BizException.badRequest("金额必须大于 0");
        }
        Long memberId = resolveMember(userId, ledger, req.getMemberId());
        validateItems(req.getType(), req.getItems(), amount);

        Bill bill = new Bill();
        bill.setLedgerId(req.getLedgerId());
        bill.setType(req.getType());
        bill.setCategoryId("transfer".equals(req.getType()) ? null : req.getCategoryId());
        bill.setMemberId(memberId);
        bill.setAmount(amount);
        bill.setBillDate(req.getBillDate() == null ? LocalDateTime.now() : req.getBillDate());
        bill.setRemark(req.getRemark());
        bill.setCreatedBy(userId);
        bill.setCreatedAt(LocalDateTime.now());
        bill.setUpdatedAt(LocalDateTime.now());
        billMapper.insert(bill);

        insertBillAccounts(bill.getId(), req.getItems());
        saveTags(bill.getId(), req.getTagIds(), req.getLedgerId());
        applyBalance(req.getItems(), req.getLedgerId());

        auditLogService.record(req.getLedgerId(), bill.getId(), userId, "create",
            describe("创建账单", bill, req.getItems()));
        return toVO(bill);
    }

    @Transactional
    public BillVO updateBill(Long userId, Long billId, BillRequest req) {
        Bill old = requireBill(billId);
        ledgerService.requireMember(old.getLedgerId(), userId);
        if (req.getLedgerId() == null) {
            req.setLedgerId(old.getLedgerId());
        }
        if (!old.getLedgerId().equals(req.getLedgerId())) {
            throw BizException.badRequest("不能跨账本修改账单");
        }
        BigDecimal amount = req.getAmount();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw BizException.badRequest("金额必须大于 0");
        }
        Ledger ledger = ledgerMapper.selectById(old.getLedgerId());
        Long memberId = resolveMember(userId, ledger, req.getMemberId());
        validateItems(req.getType(), req.getItems(), amount);

        // 回滚原账户影响
        List<BillAccount> oldItems = billAccountMapper.selectList(
            Wrappers.<BillAccount>lambdaQuery().eq(BillAccount::getBillId, billId));
        applyBalanceReverse(oldItems);

        // 保存变更前快照（必须在修改 old 之前，字段均为不可变类型）
        Map<String, Object> beforeMap = billToMap(old, oldItems);

        // 更新账单
        old.setType(req.getType());
        old.setCategoryId("transfer".equals(req.getType()) ? null : req.getCategoryId());
        old.setMemberId(memberId);
        old.setAmount(amount);
        old.setBillDate(req.getBillDate() == null ? old.getBillDate() : req.getBillDate());
        old.setRemark(req.getRemark());
        old.setUpdatedAt(LocalDateTime.now());
        billMapper.updateById(old);

        // 重建明细
        billAccountMapper.delete(Wrappers.<BillAccount>lambdaQuery().eq(BillAccount::getBillId, billId));
        billTagMapper.delete(Wrappers.<BillTag>lambdaQuery().eq(BillTag::getBillId, billId));
        insertBillAccounts(billId, req.getItems());
        saveTags(billId, req.getTagIds(), old.getLedgerId());
        applyBalance(req.getItems(), old.getLedgerId());

        auditLogService.record(old.getLedgerId(), billId, userId, "update",
            describeUpdate(beforeMap, billToMap(old, req.getItems())));
        return toVO(old);
    }

    @Transactional
    public void deleteBill(Long userId, Long billId) {
        Bill bill = requireBill(billId);
        ledgerService.requireMember(bill.getLedgerId(), userId);
        List<BillAccount> items = billAccountMapper.selectList(
            Wrappers.<BillAccount>lambdaQuery().eq(BillAccount::getBillId, billId));
        applyBalanceReverse(items);

        auditLogService.record(bill.getLedgerId(), billId, userId, "delete",
            describe("删除账单", bill, items));

        billMapper.deleteById(billId);
        billAccountMapper.delete(Wrappers.<BillAccount>lambdaQuery().eq(BillAccount::getBillId, billId));
        billTagMapper.delete(Wrappers.<BillTag>lambdaQuery().eq(BillTag::getBillId, billId));
    }

    public PageResult<BillVO> listBills(Long userId, Long ledgerId, BillQuery q) {
        ledgerService.requireMember(ledgerId, userId);
        LambdaQueryWrapper<Bill> w = Wrappers.<Bill>lambdaQuery().eq(Bill::getLedgerId, ledgerId);
        if (q.getType() != null && !q.getType().isBlank()) {
            w.eq(Bill::getType, q.getType());
        }
        if (q.getCategoryId() != null) {
            w.eq(Bill::getCategoryId, q.getCategoryId());
        }
        if (q.getMemberId() != null) {
            w.eq(Bill::getMemberId, q.getMemberId());
        }
        if (q.getStartDate() != null) {
            w.ge(Bill::getBillDate, q.getStartDate().atStartOfDay());
        }
        if (q.getEndDate() != null) {
            // 用 lt 而非 le：endDate+1天 00:00 是开区间上界，避免把次日凌晨整点的账单计入
            w.lt(Bill::getBillDate, q.getEndDate().plusDays(1).atStartOfDay());
        }
        if (q.getKeyword() != null && !q.getKeyword().isBlank()) {
            // 仅匹配当前账本的分类名，避免跨账本匹配到相同 id
            List<Long> catIds = categoryMapper.selectList(
                    Wrappers.<Category>lambdaQuery()
                        .eq(Category::getLedgerId, ledgerId)
                        .like(Category::getName, q.getKeyword()))
                .stream().map(Category::getId).toList();
            String kw = q.getKeyword();
            w.and(x -> {
                x.like(Bill::getRemark, kw);
                if (!catIds.isEmpty()) {
                    x.or().in(Bill::getCategoryId, catIds);
                }
            });
        }
        int page = Math.max(1, q.getPage());
        int size = Math.min(Math.max(1, q.getSize()), 100);

        if (q.getAccountId() != null) {
            List<Long> billIds = billAccountMapper.selectList(
                    Wrappers.<BillAccount>lambdaQuery().eq(BillAccount::getAccountId, q.getAccountId()))
                .stream().map(BillAccount::getBillId).distinct().toList();
            // 空 IN() 会生成非法 SQL 导致 500，直接返回空分页
            if (billIds.isEmpty()) {
                return new PageResult<>(List.of(), 0L, page, size);
            }
            w.in(Bill::getId, billIds);
        }
        w.orderByDesc(Bill::getBillDate).orderByDesc(Bill::getId);

        IPage<Bill> p = billMapper.selectPage(new Page<>(page, size), w);
        List<BillVO> vos = p.getRecords().stream().map(this::toVO).toList();
        return new PageResult<>(vos, p.getTotal(), page, size);
    }

    public BillVO getBill(Long userId, Long billId) {
        Bill bill = requireBill(billId);
        ledgerService.requireMember(bill.getLedgerId(), userId);
        return toVO(bill);
    }

    public List<BillLogVO> billLogs(Long userId, Long billId) {
        Bill bill = requireBill(billId);
        ledgerService.requireMember(bill.getLedgerId(), userId);
        return auditLogService.listForBill(billId).stream().map(this::toLogVO).toList();
    }

    // ---------- 内部方法 ----------

    private Bill requireBill(Long billId) {
        Bill bill = billMapper.selectById(billId);
        if (bill == null) {
            throw BizException.notFound("账单不存在");
        }
        return bill;
    }

    private Long resolveMember(Long userId, Ledger ledger, Long requestedMemberId) {
        Long memberId = requestedMemberId == null ? userId : requestedMemberId;
        if ("personal".equals(ledger.getType())) {
            if (!memberId.equals(userId)) {
                throw BizException.badRequest("个人账本只能记录自己的账");
            }
            return memberId;
        }
        ledgerService.requireMember(ledger.getId(), memberId);
        return memberId;
    }

    private void validateItems(String type, List<BillAccountItem> items, BigDecimal amount) {
        if (items == null || items.isEmpty()) {
            throw BizException.badRequest("至少需要一个账户");
        }
        BigDecimal sum = BigDecimal.ZERO;
        if ("expense".equals(type) || "income".equals(type)) {
            String expected = "expense".equals(type) ? "out" : "in";
            for (BillAccountItem it : items) {
                if (it.getAmount() == null || it.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                    throw BizException.badRequest("账户金额必须大于 0");
                }
                if (!expected.equals(it.getDirection())) {
                    throw BizException.badRequest("账户明细方向应为 " + ("out".equals(expected) ? "支出" : "收入"));
                }
                sum = sum.add(it.getAmount());
            }
            if (sum.compareTo(amount) != 0) {
                throw BizException.badRequest("账户金额合计必须等于账单金额");
            }
        } else if ("transfer".equals(type)) {
            BigDecimal sumOut = BigDecimal.ZERO;
            BigDecimal sumIn = BigDecimal.ZERO;
            java.util.Set<Long> outAccounts = new java.util.HashSet<>();
            java.util.Set<Long> inAccounts = new java.util.HashSet<>();
            for (BillAccountItem it : items) {
                if (it.getAmount() == null || it.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                    throw BizException.badRequest("账户金额必须大于 0");
                }
                if ("out".equals(it.getDirection())) {
                    sumOut = sumOut.add(it.getAmount());
                    outAccounts.add(it.getAccountId());
                } else if ("in".equals(it.getDirection())) {
                    sumIn = sumIn.add(it.getAmount());
                    inAccounts.add(it.getAccountId());
                } else {
                    throw BizException.badRequest("转账明细方向只能是 out 或 in");
                }
            }
            if (sumOut.compareTo(amount) != 0 || sumIn.compareTo(amount) != 0) {
                throw BizException.badRequest("转账转出与转入金额必须等于账单金额");
            }
            // 转出账户与转入账户不能重叠（同一账户不可既转出又转入，否则余额对冲 = 白记）
            outAccounts.retainAll(inAccounts);
            if (!outAccounts.isEmpty()) {
                throw BizException.badRequest("转账的转出账户与转入账户不能是同一个账户");
            }
        } else {
            throw BizException.badRequest("账单类型不合法");
        }
    }

    private void insertBillAccounts(Long billId, List<BillAccountItem> items) {
        for (BillAccountItem it : items) {
            BillAccount ba = new BillAccount();
            ba.setBillId(billId);
            ba.setAccountId(it.getAccountId());
            ba.setDirection(it.getDirection());
            ba.setAmount(it.getAmount());
            // 转账配对：同一账单的转出/转入共享 pairId（此处用 billId）
            ba.setPairId(billId);
            billAccountMapper.insert(ba);
        }
    }

    /**
     * 校验标签属于当前账本并保存关联。
     */
    private void saveTags(Long billId, List<Long> tagIds, Long ledgerId) {
        if (tagIds == null) {
            return;
        }
        for (Long tagId : tagIds) {
            Tag tag = tagMapper.selectById(tagId);
            if (tag == null || !tag.getLedgerId().equals(ledgerId)) {
                throw BizException.badRequest("标签不存在或不属于该账本");
            }
            BillTag bt = new BillTag();
            bt.setBillId(billId);
            bt.setTagId(tagId);
            billTagMapper.insert(bt);
        }
    }

    private void applyBalance(List<BillAccountItem> items, Long ledgerId) {
        for (BillAccountItem it : items) {
            Account acc = accountMapper.selectById(it.getAccountId());
            if (acc == null || !acc.getLedgerId().equals(ledgerId)) {
                throw BizException.badRequest("账户不存在或不属于该账本");
            }
            BigDecimal delta = "in".equals(it.getDirection()) ? it.getAmount() : it.getAmount().negate();
            if ("credit".equals(acc.getType())) {
                delta = delta.negate();
            }
            // 原子更新 balance = balance + delta，避免并发读-改-写丢失更新
            accountMapper.addBalance(acc.getId(), delta);
        }
    }

    /** 回滚原明细对账户余额的影响 */
    private void applyBalanceReverse(List<BillAccount> items) {
        for (BillAccount it : items) {
            Account acc = accountMapper.selectById(it.getAccountId());
            if (acc == null) {
                continue;
            }
            String reversed = "in".equals(it.getDirection()) ? "out" : "in";
            BigDecimal delta = "in".equals(reversed) ? it.getAmount() : it.getAmount().negate();
            if ("credit".equals(acc.getType())) {
                delta = delta.negate();
            }
            // 原子更新，避免并发丢失
            accountMapper.addBalance(acc.getId(), delta);
        }
    }

    private BillVO toVO(Bill bill) {
        BillVO vo = new BillVO();
        vo.setId(bill.getId());
        vo.setLedgerId(bill.getLedgerId());
        vo.setType(bill.getType());
        vo.setCategoryId(bill.getCategoryId());
        if (bill.getCategoryId() != null) {
            Category cat = categoryMapper.selectById(bill.getCategoryId());
            if (cat != null) {
                vo.setCategoryName(cat.getName());
                vo.setCategoryIcon(cat.getIcon());
            }
        }
        vo.setMemberId(bill.getMemberId());
        if (bill.getMemberId() != null) {
            User u = userService.getById(bill.getMemberId());
            vo.setMemberName(u.getNickname());
        }
        vo.setAmount(bill.getAmount());
        vo.setBillDate(bill.getBillDate());
        vo.setRemark(bill.getRemark());
        vo.setCreatedAt(bill.getCreatedAt());
        vo.setUpdatedAt(bill.getUpdatedAt());

        List<BillTag> bts = billTagMapper.selectList(
            Wrappers.<BillTag>lambdaQuery().eq(BillTag::getBillId, bill.getId()));
        List<BillVO.TagVO> tags = bts.stream().map(bt -> {
            Tag t = tagMapper.selectById(bt.getTagId());
            return new BillVO.TagVO(t.getId(), t.getName(), t.getColor());
        }).toList();
        vo.setTags(tags);

        List<BillAccount> bas = billAccountMapper.selectList(
            Wrappers.<BillAccount>lambdaQuery().eq(BillAccount::getBillId, bill.getId()));
        List<BillVO.BillAccountVO> accounts = bas.stream().map(ba -> {
            Account a = accountMapper.selectById(ba.getAccountId());
            return new BillVO.BillAccountVO(ba.getAccountId(),
                a == null ? "" : a.getName(), ba.getDirection(), ba.getAmount(), ba.getPairId());
        }).toList();
        vo.setAccounts(accounts);
        return vo;
    }

    /** 把账单 + 账户明细转为可序列化的 Map（字段均为不可变类型，调用后修改原对象不影响此 Map） */
    private Map<String, Object> billToMap(Bill bill, List<?> items) {
        Map<String, Object> detail = new HashMap<>();
        detail.put("type", bill.getType());
        detail.put("amount", bill.getAmount());
        detail.put("categoryId", bill.getCategoryId());
        detail.put("memberId", bill.getMemberId());
        detail.put("billDate", bill.getBillDate());
        detail.put("remark", bill.getRemark());
        List<Map<String, Object>> itemList = new ArrayList<>();
        for (Object o : items) {
            if (o instanceof BillAccountItem ba) {
                itemList.add(Map.of("accountId", ba.getAccountId(), "direction", ba.getDirection(), "amount", ba.getAmount()));
            } else if (o instanceof BillAccount ba) {
                itemList.add(Map.of("accountId", ba.getAccountId(), "direction", ba.getDirection(), "amount", ba.getAmount()));
            }
        }
        detail.put("accounts", itemList);
        return detail;
    }

    private String describe(String action, Bill bill, List<?> items) {
        try {
            Map<String, Object> detail = billToMap(bill, items);
            detail.put("action", action);
            return objectMapper.writeValueAsString(detail);
        } catch (Exception e) {
            return "{}";
        }
    }

    /** 编辑留痕：同时记录变更前与变更后 */
    private String describeUpdate(Map<String, Object> before, Map<String, Object> after) {
        try {
            Map<String, Object> detail = new HashMap<>();
            detail.put("action", "修改账单");
            detail.put("before", before);
            detail.put("after", after);
            return objectMapper.writeValueAsString(detail);
        } catch (Exception e) {
            return "{}";
        }
    }

    /** 把审计日志转成详情页展示用的 VO：补充操作人昵称 + 生成可读摘要 */
    private BillLogVO toLogVO(AuditLog log) {
        String operatorName = "";
        if (log.getOperatorId() != null) {
            try {
                operatorName = userService.getById(log.getOperatorId()).getNickname();
            } catch (Exception ignored) {
                operatorName = "";
            }
        }
        return new BillLogVO(log.getId(), log.getAction(), operatorName,
            buildLogSummary(log.getChangeDetail()), log.getCreatedAt());
    }

    /** 从 changeDetail JSON 生成人可读的摘要，如「支出 ¥30.00 · 微信 · 午餐」 */
    private String buildLogSummary(String changeDetail) {
        if (changeDetail == null || changeDetail.isBlank()) {
            return "";
        }
        try {
            Map<String, Object> d = objectMapper.readValue(changeDetail, new TypeReference<Map<String, Object>>() { });
            // 兼容 update 的 before/after 格式：摘要展示变更后的状态
            Object afterObj = d.get("after");
            if (afterObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> after = (Map<String, Object>) afterObj;
                d = after;
            }
            String type = d.get("type") == null ? "" : d.get("type").toString();
            String typeCn = switch (type) {
                case "expense" -> "支出";
                case "income" -> "收入";
                case "transfer" -> "转账";
                default -> "账单";
            };
            StringBuilder sb = new StringBuilder(typeCn);
            if (d.get("amount") != null) {
                sb.append(" ¥").append(new BigDecimal(d.get("amount").toString()).stripTrailingZeros().toPlainString());
            }
            Object accountsObj = d.get("accounts");
            if (accountsObj instanceof List<?> accounts && !accounts.isEmpty()) {
                List<String> names = new ArrayList<>();
                for (Object o : accounts) {
                    if (o instanceof Map<?, ?> m && m.get("accountId") != null) {
                        Account acc = accountMapper.selectById(Long.valueOf(m.get("accountId").toString()));
                        if (acc != null) {
                            names.add(acc.getName());
                        }
                    }
                }
                if (!names.isEmpty()) {
                    sb.append(" · ").append(String.join("、", names));
                }
            }
            if (d.get("remark") != null && !d.get("remark").toString().isBlank()) {
                sb.append(" · ").append(d.get("remark"));
            }
            return sb.toString();
        } catch (Exception e) {
            return changeDetail;
        }
    }
}
