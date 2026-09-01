package com.familyhome.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.familyhome.common.BizException;
import com.familyhome.dto.CreateLedgerRequest;
import com.familyhome.dto.LedgerVO;
import com.familyhome.entity.Account;
import com.familyhome.entity.AuditLog;
import com.familyhome.entity.Bill;
import com.familyhome.entity.BillAccount;
import com.familyhome.entity.BillTag;
import com.familyhome.entity.Budget;
import com.familyhome.entity.Category;
import com.familyhome.entity.FamilyMember;
import com.familyhome.entity.Ledger;
import com.familyhome.entity.LedgerMember;
import com.familyhome.entity.Tag;
import com.familyhome.entity.User;
import com.familyhome.mapper.AccountMapper;
import com.familyhome.mapper.AuditLogMapper;
import com.familyhome.mapper.BillAccountMapper;
import com.familyhome.mapper.BillMapper;
import com.familyhome.mapper.BillTagMapper;
import com.familyhome.mapper.BudgetMapper;
import com.familyhome.mapper.CategoryMapper;
import com.familyhome.mapper.FamilyMemberMapper;
import com.familyhome.mapper.LedgerMapper;
import com.familyhome.mapper.LedgerMemberMapper;
import com.familyhome.mapper.TagMapper;
import com.familyhome.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class LedgerService {

    private static final String[][] DEFAULT_EXPENSE = {
        {"餐饮", "food"}, {"交通", "traffic"}, {"购物", "shopping"}, {"居住", "home"},
        {"娱乐", "fun"}, {"医疗", "medical"}, {"教育", "edu"}, {"人情", "gift"}, {"其他", "other"}
    };
    private static final String[][] DEFAULT_INCOME = {
        {"工资", "salary"}, {"奖金", "bonus"}, {"理财", "invest"}, {"兼职", "parttime"},
        {"红包", "redpacket"}, {"其他", "other"}
    };

    private final LedgerMapper ledgerMapper;
    private final LedgerMemberMapper ledgerMemberMapper;
    private final CategoryMapper categoryMapper;
    private final FamilyMemberMapper familyMemberMapper;
    private final FamilyService familyService;
    private final BillMapper billMapper;
    private final BillAccountMapper billAccountMapper;
    private final BillTagMapper billTagMapper;
    private final AccountMapper accountMapper;
    private final BudgetMapper budgetMapper;
    private final AuditLogMapper auditLogMapper;
    private final TagMapper tagMapper;
    private final UserMapper userMapper;

    public LedgerService(LedgerMapper ledgerMapper, LedgerMemberMapper ledgerMemberMapper,
                         CategoryMapper categoryMapper, FamilyMemberMapper familyMemberMapper,
                         FamilyService familyService, BillMapper billMapper,
                         BillAccountMapper billAccountMapper, BillTagMapper billTagMapper,
                         AccountMapper accountMapper, BudgetMapper budgetMapper,
                         AuditLogMapper auditLogMapper, TagMapper tagMapper, UserMapper userMapper) {
        this.ledgerMapper = ledgerMapper;
        this.ledgerMemberMapper = ledgerMemberMapper;
        this.categoryMapper = categoryMapper;
        this.familyMemberMapper = familyMemberMapper;
        this.familyService = familyService;
        this.billMapper = billMapper;
        this.billAccountMapper = billAccountMapper;
        this.billTagMapper = billTagMapper;
        this.accountMapper = accountMapper;
        this.budgetMapper = budgetMapper;
        this.auditLogMapper = auditLogMapper;
        this.tagMapper = tagMapper;
        this.userMapper = userMapper;
    }

    @Transactional
    public LedgerVO createLedger(Long userId, CreateLedgerRequest req) {
        Ledger ledger = new Ledger();
        ledger.setName(req.getName());
        ledger.setType(req.getType());
        ledger.setIcon(req.getIcon() == null ? "book" : req.getIcon());
        ledger.setTheme(req.getTheme() == null ? "default" : req.getTheme());
        ledger.setOwnerId(userId);
        ledger.setStatus(1);
        // 手动新建的账本非默认（仅注册时自动创建的"个人账本"为默认）
        ledger.setIsDefault(0);
        ledger.setCreatedAt(LocalDateTime.now());

        if ("personal".equals(req.getType())) {
            ledger.setFamilyId(null);
            ledgerMapper.insert(ledger);
            insertMember(ledger.getId(), userId, "creator");
        } else {
            Long familyId = familyService.findMyFamilyId(userId);
            if (familyId == null) {
                throw BizException.badRequest("请先创建或加入家庭，再创建公共账本");
            }
            ledger.setFamilyId(familyId);
            ledgerMapper.insert(ledger);
            // 公共账本：家庭成员全部加入
            List<FamilyMember> members = familyMemberMapper.selectList(
                Wrappers.<FamilyMember>lambdaQuery().eq(FamilyMember::getFamilyId, familyId));
            for (FamilyMember fm : members) {
                insertMember(ledger.getId(), fm.getUserId(),
                    fm.getUserId().equals(userId) ? "creator" : "member");
            }
        }
        seedDefaultCategories(ledger.getId());
        return toVO(ledger, userId);
    }

    public List<LedgerVO> listLedgers(Long userId) {
        List<LedgerMember> lms = ledgerMemberMapper.selectList(
            Wrappers.<LedgerMember>lambdaQuery().eq(LedgerMember::getUserId, userId));
        return lms.stream().map(lm -> {
            Ledger ledger = ledgerMapper.selectById(lm.getLedgerId());
            if (ledger == null) {
                return null;
            }
            LedgerVO vo = toVO(ledger, userId);
            vo.setRole(lm.getRole());
            return vo;
        }).filter(java.util.Objects::nonNull).toList();
    }

    /**
     * 新用户注册后自动创建默认账本：类型个人、名称"个人账本"、默认标记。
     */
    @Transactional
    public LedgerVO createDefaultLedgerForNewUser(Long userId) {
        Ledger ledger = new Ledger();
        ledger.setName("个人账本");
        ledger.setType("personal");
        ledger.setIcon("book");
        ledger.setTheme("default");
        ledger.setOwnerId(userId);
        ledger.setFamilyId(null);
        ledger.setStatus(1);
        ledger.setIsDefault(1);
        ledger.setCreatedAt(LocalDateTime.now());
        ledgerMapper.insert(ledger);
        insertMember(ledger.getId(), userId, "creator");
        seedDefaultCategories(ledger.getId());
        return toVO(ledger, userId);
    }

    /**
     * 切换默认账本：先把当前用户可见账本的默认标记清除，再将目标账本设为默认。
     * 目标账本必须是当前用户是成员的账本。
     */
    @Transactional
    public LedgerVO setDefaultLedger(Long userId, Long ledgerId) {
        Ledger target = ledgerMapper.selectById(ledgerId);
        if (target == null) {
            throw BizException.notFound("账本不存在");
        }
        requireMember(ledgerId, userId);
        // 清除当前用户可见账本的默认标记
        List<LedgerMember> lms = ledgerMemberMapper.selectList(
            Wrappers.<LedgerMember>lambdaQuery().eq(LedgerMember::getUserId, userId));
        for (LedgerMember lm : lms) {
            Ledger l = ledgerMapper.selectById(lm.getLedgerId());
            if (l != null && Integer.valueOf(1).equals(l.getIsDefault())) {
                l.setIsDefault(0);
                ledgerMapper.updateById(l);
            }
        }
        // 设置目标账本为默认
        target.setIsDefault(1);
        ledgerMapper.updateById(target);
        return toVO(target, userId);
    }

    public LedgerVO getLedger(Long userId, Long ledgerId) {
        requireMember(ledgerId, userId);
        Ledger ledger = ledgerMapper.selectById(ledgerId);
        if (ledger == null) {
            throw BizException.notFound("账本不存在");
        }
        return toVO(ledger, userId);
    }

    @Transactional
    public void deleteLedger(Long userId, Long ledgerId) {
        Ledger ledger = ledgerMapper.selectById(ledgerId);
        if (ledger == null) {
            throw BizException.notFound("账本不存在");
        }
        if (!ledger.getOwnerId().equals(userId)) {
            throw BizException.forbidden("仅账本创建者可删除账本");
        }

        // 级联清理：先删子表关联，再删主表，避免孤儿数据
        // 1. 账单账户明细 & 账单标签关联
        billAccountMapper.delete(Wrappers.<BillAccount>lambdaQuery()
            .inSql(BillAccount::getBillId, "SELECT id FROM t_bill WHERE ledger_id = " + ledgerId));
        billTagMapper.delete(Wrappers.<BillTag>lambdaQuery()
            .inSql(BillTag::getBillId, "SELECT id FROM t_bill WHERE ledger_id = " + ledgerId));
        // 2. 账单
        billMapper.delete(Wrappers.<Bill>lambdaQuery().eq(Bill::getLedgerId, ledgerId));
        // 3. 账户
        accountMapper.delete(Wrappers.<Account>lambdaQuery().eq(Account::getLedgerId, ledgerId));
        // 4. 预算
        budgetMapper.delete(Wrappers.<Budget>lambdaQuery().eq(Budget::getLedgerId, ledgerId));
        // 5. 审计日志
        auditLogMapper.delete(Wrappers.<AuditLog>lambdaQuery().eq(AuditLog::getLedgerId, ledgerId));
        // 6. 分类 & 标签
        categoryMapper.delete(Wrappers.<Category>lambdaQuery().eq(Category::getLedgerId, ledgerId));
        tagMapper.delete(Wrappers.<Tag>lambdaQuery().eq(Tag::getLedgerId, ledgerId));
        // 7. 账本成员 & 账本本身
        ledgerMemberMapper.delete(Wrappers.<LedgerMember>lambdaQuery()
            .eq(LedgerMember::getLedgerId, ledgerId));
        ledgerMapper.deleteById(ledgerId);
    }

    public void requireMember(Long ledgerId, Long userId) {
        Long count = ledgerMemberMapper.selectCount(
            Wrappers.<LedgerMember>lambdaQuery()
                .eq(LedgerMember::getLedgerId, ledgerId)
                .eq(LedgerMember::getUserId, userId));
        if (count == null || count == 0) {
            throw BizException.forbidden("您不是该账本成员");
        }
    }

    public void requireCreator(Long ledgerId, Long userId) {
        LedgerMember lm = ledgerMemberMapper.selectOne(
            Wrappers.<LedgerMember>lambdaQuery()
                .eq(LedgerMember::getLedgerId, ledgerId)
                .eq(LedgerMember::getUserId, userId)
                .last("limit 1"));
        if (lm == null) {
            throw BizException.forbidden("您不是该账本成员");
        }
        if (!"creator".equals(lm.getRole())) {
            throw BizException.forbidden("仅账本创建者可执行此操作");
        }
    }

    /** 账本创建者添加成员（仅公共账本；个人账本保持私有） */
    @Transactional
    public void addMember(Long operatorId, Long ledgerId, Long userId) {
        Ledger ledger = ledgerMapper.selectById(ledgerId);
        if (ledger == null) {
            throw BizException.notFound("账本不存在");
        }
        requireCreator(ledgerId, operatorId);
        if (!"public".equals(ledger.getType())) {
            throw BizException.badRequest("仅公共账本支持添加成员");
        }
        if (userMapper.selectById(userId) == null) {
            throw BizException.notFound("用户不存在");
        }
        Long exists = ledgerMemberMapper.selectCount(
            Wrappers.<LedgerMember>lambdaQuery()
                .eq(LedgerMember::getLedgerId, ledgerId)
                .eq(LedgerMember::getUserId, userId));
        if (exists != null && exists > 0) {
            throw BizException.badRequest("该用户已是账本成员");
        }
        insertMember(ledgerId, userId, "member");
    }

    /** 账本创建者移除成员（不能移除创建者本人） */
    @Transactional
    public void removeMember(Long operatorId, Long ledgerId, Long userId) {
        Ledger ledger = ledgerMapper.selectById(ledgerId);
        if (ledger == null) {
            throw BizException.notFound("账本不存在");
        }
        requireCreator(ledgerId, operatorId);
        if (ledger.getOwnerId().equals(userId)) {
            throw BizException.badRequest("不能移除账本创建者");
        }
        int removed = ledgerMemberMapper.delete(
            Wrappers.<LedgerMember>lambdaQuery()
                .eq(LedgerMember::getLedgerId, ledgerId)
                .eq(LedgerMember::getUserId, userId));
        if (removed == 0) {
            throw BizException.notFound("该用户不是账本成员");
        }
    }

    /** 家庭新成员加入后，自动加入该家庭的公共账本 */
    @Transactional
    public void addFamilyMemberToPublicLedgers(Long familyId, Long userId) {
        List<Ledger> publicLedgers = ledgerMapper.selectList(
            Wrappers.<Ledger>lambdaQuery()
                .eq(Ledger::getFamilyId, familyId)
                .eq(Ledger::getType, "public"));
        for (Ledger ledger : publicLedgers) {
            Long exists = ledgerMemberMapper.selectCount(
                Wrappers.<LedgerMember>lambdaQuery()
                    .eq(LedgerMember::getLedgerId, ledger.getId())
                    .eq(LedgerMember::getUserId, userId));
            if (exists == null || exists == 0) {
                insertMember(ledger.getId(), userId, "member");
            }
        }
    }

    private void insertMember(Long ledgerId, Long userId, String role) {
        LedgerMember lm = new LedgerMember();
        lm.setLedgerId(ledgerId);
        lm.setUserId(userId);
        lm.setRole(role);
        lm.setJoinedAt(LocalDateTime.now());
        ledgerMemberMapper.insert(lm);
    }

    private void seedDefaultCategories(Long ledgerId) {
        int sort = 0;
        for (String[] c : DEFAULT_EXPENSE) {
            Category cat = new Category();
            cat.setLedgerId(ledgerId);
            cat.setType("expense");
            cat.setName(c[0]);
            cat.setIcon(c[1]);
            cat.setSort(sort++);
            cat.setEnabled(1);
            cat.setCreatedAt(LocalDateTime.now());
            categoryMapper.insert(cat);
        }
        for (String[] c : DEFAULT_INCOME) {
            Category cat = new Category();
            cat.setLedgerId(ledgerId);
            cat.setType("income");
            cat.setName(c[0]);
            cat.setIcon(c[1]);
            cat.setSort(sort++);
            cat.setEnabled(1);
            cat.setCreatedAt(LocalDateTime.now());
            categoryMapper.insert(cat);
        }
    }

    private LedgerVO toVO(Ledger ledger, Long userId) {
        Long memberCount = ledgerMemberMapper.selectCount(
            Wrappers.<LedgerMember>lambdaQuery().eq(LedgerMember::getLedgerId, ledger.getId()));
        LedgerMember self = ledgerMemberMapper.selectOne(
            Wrappers.<LedgerMember>lambdaQuery()
                .eq(LedgerMember::getLedgerId, ledger.getId())
                .eq(LedgerMember::getUserId, userId)
                .last("limit 1"));
        return new LedgerVO(ledger.getId(), ledger.getName(), ledger.getType(), ledger.getIcon(),
            ledger.getTheme(), ledger.getOwnerId(), ledger.getFamilyId(),
            self == null ? "member" : self.getRole(), memberCount, ledger.getIsDefault());
    }
}
