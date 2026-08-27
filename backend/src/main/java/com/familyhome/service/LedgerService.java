package com.familyhome.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.familyhome.common.BizException;
import com.familyhome.dto.CreateLedgerRequest;
import com.familyhome.dto.LedgerVO;
import com.familyhome.entity.Category;
import com.familyhome.entity.FamilyMember;
import com.familyhome.entity.Ledger;
import com.familyhome.entity.LedgerMember;
import com.familyhome.mapper.CategoryMapper;
import com.familyhome.mapper.FamilyMemberMapper;
import com.familyhome.mapper.LedgerMapper;
import com.familyhome.mapper.LedgerMemberMapper;
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

    public LedgerService(LedgerMapper ledgerMapper, LedgerMemberMapper ledgerMemberMapper,
                         CategoryMapper categoryMapper, FamilyMemberMapper familyMemberMapper,
                         FamilyService familyService) {
        this.ledgerMapper = ledgerMapper;
        this.ledgerMemberMapper = ledgerMemberMapper;
        this.categoryMapper = categoryMapper;
        this.familyMemberMapper = familyMemberMapper;
        this.familyService = familyService;
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
        ledgerMapper.deleteById(ledgerId);
        ledgerMemberMapper.delete(Wrappers.<LedgerMember>lambdaQuery()
            .eq(LedgerMember::getLedgerId, ledgerId));
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
            self == null ? "member" : self.getRole(), memberCount);
    }
}
