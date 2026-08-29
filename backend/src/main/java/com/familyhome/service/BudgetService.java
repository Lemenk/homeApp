package com.familyhome.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.familyhome.common.BizException;
import com.familyhome.dto.CreateBudgetRequest;
import com.familyhome.entity.Bill;
import com.familyhome.entity.Budget;
import com.familyhome.entity.Category;
import com.familyhome.mapper.BillMapper;
import com.familyhome.mapper.BudgetMapper;
import com.familyhome.mapper.CategoryMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class BudgetService {

    private final BudgetMapper budgetMapper;
    private final BillMapper billMapper;
    private final CategoryMapper categoryMapper;
    private final LedgerService ledgerService;

    public BudgetService(BudgetMapper budgetMapper, BillMapper billMapper,
                         CategoryMapper categoryMapper, LedgerService ledgerService) {
        this.budgetMapper = budgetMapper;
        this.billMapper = billMapper;
        this.categoryMapper = categoryMapper;
        this.ledgerService = ledgerService;
    }

    @Transactional
    public Map<String, Object> create(Long userId, Long ledgerId, CreateBudgetRequest req) {
        ledgerService.requireMember(ledgerId, userId);
        validateBudget(ledgerId, req);

        Long exists = budgetMapper.selectCount(Wrappers.<Budget>lambdaQuery()
            .eq(Budget::getLedgerId, ledgerId)
            .eq(Budget::getCategoryId, req.getCategoryId()));
        if (exists != null && exists > 0) {
            throw BizException.badRequest("该分类已设置预算，请先修改或删除");
        }

        Budget b = new Budget();
        b.setLedgerId(ledgerId);
        b.setCategoryId(req.getCategoryId());
        b.setPeriodType(req.getPeriodType());
        if ("custom".equals(req.getPeriodType())) {
            if (req.getStartDate() == null || req.getEndDate() == null) {
                throw BizException.badRequest("自定义周期需要起止日期");
            }
            if (req.getStartDate().isAfter(req.getEndDate())) {
                throw BizException.badRequest("开始日期不能晚于结束日期");
            }
            b.setStartDate(req.getStartDate());
            b.setEndDate(req.getEndDate());
        }
        b.setAmount(req.getAmount());
        b.setRemark(req.getRemark());
        b.setCreatedAt(LocalDateTime.now());
        b.setUpdatedAt(LocalDateTime.now());
        budgetMapper.insert(b);
        return toVO(b);
    }

    @Transactional
    public Map<String, Object> update(Long userId, Long budgetId, CreateBudgetRequest req) {
        Budget old = budgetMapper.selectById(budgetId);
        if (old == null) {
            throw BizException.notFound("预算不存在");
        }
        ledgerService.requireMember(old.getLedgerId(), userId);
        validateBudget(old.getLedgerId(), req);

        // 排除自身后仍重复则拒绝
        Long exists = budgetMapper.selectCount(Wrappers.<Budget>lambdaQuery()
            .eq(Budget::getLedgerId, old.getLedgerId())
            .eq(Budget::getCategoryId, req.getCategoryId())
            .ne(Budget::getId, budgetId));
        if (exists != null && exists > 0) {
            throw BizException.badRequest("该分类已设置预算");
        }

        old.setCategoryId(req.getCategoryId());
        old.setPeriodType(req.getPeriodType());
        if ("custom".equals(req.getPeriodType())) {
            if (req.getStartDate() == null || req.getEndDate() == null) {
                throw BizException.badRequest("自定义周期需要起止日期");
            }
            if (req.getStartDate().isAfter(req.getEndDate())) {
                throw BizException.badRequest("开始日期不能晚于结束日期");
            }
            old.setStartDate(req.getStartDate());
            old.setEndDate(req.getEndDate());
        } else {
            old.setStartDate(null);
            old.setEndDate(null);
        }
        old.setAmount(req.getAmount());
        old.setRemark(req.getRemark());
        old.setUpdatedAt(LocalDateTime.now());
        budgetMapper.updateById(old);
        return toVO(old);
    }

    @Transactional
    public void delete(Long userId, Long budgetId) {
        Budget b = budgetMapper.selectById(budgetId);
        if (b == null) {
            throw BizException.notFound("预算不存在");
        }
        ledgerService.requireMember(b.getLedgerId(), userId);
        budgetMapper.deleteById(budgetId);
    }

    public List<Map<String, Object>> list(Long userId, Long ledgerId) {
        ledgerService.requireMember(ledgerId, userId);
        List<Budget> budgets = budgetMapper.selectList(
            Wrappers.<Budget>lambdaQuery().eq(Budget::getLedgerId, ledgerId)
                .orderByAsc(Budget::getId));
        return budgets.stream().map(this::toVO).toList();
    }

    private void validateBudget(Long ledgerId, CreateBudgetRequest req) {
        if (req.getAmount() == null || req.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw BizException.badRequest("预算金额必须大于 0");
        }
        Category cat = categoryMapper.selectById(req.getCategoryId());
        if (cat == null || !cat.getLedgerId().equals(ledgerId)) {
            throw BizException.badRequest("分类不存在或不属于该账本");
        }
    }

    private Map<String, Object> toVO(Budget b) {
        Map<String, Object> vo = new LinkedHashMap<>();
        vo.put("id", b.getId());
        vo.put("ledgerId", b.getLedgerId());
        vo.put("categoryId", b.getCategoryId());
        Category cat = categoryMapper.selectById(b.getCategoryId());
        vo.put("categoryName", cat == null ? "" : cat.getName());
        vo.put("categoryIcon", cat == null ? "" : cat.getIcon());
        vo.put("periodType", b.getPeriodType());
        vo.put("startDate", b.getStartDate());
        vo.put("endDate", b.getEndDate());
        vo.put("amount", b.getAmount());
        vo.put("remark", b.getRemark() == null ? "" : b.getRemark());

        // 进度计算
        LocalDate[] range = periodRange(b);
        BigDecimal usage = sumExpense(b.getLedgerId(), b.getCategoryId(), range[0], range[1]);
        vo.put("usage", usage);
        vo.put("percent", calcPercent(usage, b.getAmount()));
        vo.put("overBudget", usage.compareTo(b.getAmount()) > 0);
        return vo;
    }

    private LocalDate[] periodRange(Budget b) {
        LocalDate now = LocalDate.now();
        if ("custom".equals(b.getPeriodType())) {
            return new LocalDate[]{b.getStartDate(), b.getEndDate()};
        }
        YearMonth ym = YearMonth.from(now);
        return new LocalDate[]{ym.atDay(1), ym.atEndOfMonth()};
    }

    private BigDecimal sumExpense(Long ledgerId, Long categoryId, LocalDate start, LocalDate end) {
        List<Bill> bills = billMapper.selectList(Wrappers.<Bill>lambdaQuery()
            .eq(Bill::getLedgerId, ledgerId)
            .eq(Bill::getType, "expense")
            .eq(Bill::getCategoryId, categoryId)
            .ge(Bill::getBillDate, start.atStartOfDay())
            // 用 lt 而非 le：end+1天 00:00 是开区间上界，避免把次日凌晨整点的账单计入
            .lt(Bill::getBillDate, end.plusDays(1).atStartOfDay()));
        BigDecimal sum = bills.stream()
            .map(Bill::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum;
    }

    private BigDecimal calcPercent(BigDecimal usage, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return usage.multiply(BigDecimal.valueOf(100)).divide(amount, 1, RoundingMode.HALF_UP);
    }
}
