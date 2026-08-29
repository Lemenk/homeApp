package com.familyhome.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.familyhome.common.BizException;
import com.familyhome.entity.Bill;
import com.familyhome.entity.Category;
import com.familyhome.mapper.BillMapper;
import com.familyhome.mapper.CategoryMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class StatisticsService {

    private final BillMapper billMapper;
    private final CategoryMapper categoryMapper;
    private final LedgerService ledgerService;

    public StatisticsService(BillMapper billMapper, CategoryMapper categoryMapper,
                             LedgerService ledgerService) {
        this.billMapper = billMapper;
        this.categoryMapper = categoryMapper;
        this.ledgerService = ledgerService;
    }

    /**
     * 收支趋势：按 日/月 聚合指定范围内的收入与支出（不含转账）。
     */
    public List<Map<String, Object>> trend(Long userId, Long ledgerId, LocalDate start, LocalDate end,
                                           String groupBy) {
        ledgerService.requireMember(ledgerId, userId);
        LocalDate s = start == null ? LocalDate.now().minusMonths(5).withDayOfMonth(1) : start;
        LocalDate e = end == null ? LocalDate.now() : end;
        if (s.isAfter(e)) {
            throw BizException.badRequest("开始日期不能晚于结束日期");
        }
        String granularity = "day".equals(groupBy) ? "day" : "month";

        List<Bill> bills = billMapper.selectList(Wrappers.<Bill>lambdaQuery()
            .eq(Bill::getLedgerId, ledgerId)
            .in(Bill::getType, "expense", "income")
            .ge(Bill::getBillDate, s.atStartOfDay())
            .lt(Bill::getBillDate, e.plusDays(1).atStartOfDay()));

        Map<String, BigDecimal[]> bucket = new LinkedHashMap<>();
        for (Bill b : bills) {
            String key = "day".equals(granularity)
                ? b.getBillDate().toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE)
                : b.getBillDate().toLocalDate().format(DateTimeFormatter.ofPattern("yyyy-MM"));
            BigDecimal[] row = bucket.computeIfAbsent(key, k -> new BigDecimal[]{
                BigDecimal.ZERO, BigDecimal.ZERO});
            if ("income".equals(b.getType())) {
                row[1] = row[1].add(b.getAmount());
            } else {
                row[0] = row[0].add(b.getAmount());
            }
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, BigDecimal[]> entry : bucket.entrySet()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("period", entry.getKey());
            m.put("expense", entry.getValue()[0]);
            m.put("income", entry.getValue()[1]);
            result.add(m);
        }
        result.sort((a, b) -> ((String) a.get("period")).compareTo((String) b.get("period")));
        return result;
    }

    /**
     * 分类占比：按 支出/收入 类型聚合金额及占比（不含转账）。
     */
    public List<Map<String, Object>> category(Long userId, Long ledgerId, String type,
                                              LocalDate start, LocalDate end) {
        ledgerService.requireMember(ledgerId, userId);
        if (!"expense".equals(type) && !"income".equals(type)) {
            throw BizException.badRequest("类型只能是 expense 或 income");
        }
        LocalDate s = start == null ? LocalDate.now().minusMonths(5).withDayOfMonth(1) : start;
        LocalDate e = end == null ? LocalDate.now() : end;
        if (s.isAfter(e)) {
            throw BizException.badRequest("开始日期不能晚于结束日期");
        }

        List<Bill> bills = billMapper.selectList(Wrappers.<Bill>lambdaQuery()
            .eq(Bill::getLedgerId, ledgerId)
            .eq(Bill::getType, type)
            .ge(Bill::getBillDate, s.atStartOfDay())
            .lt(Bill::getBillDate, e.plusDays(1).atStartOfDay()));

        Map<Long, BigDecimal> byCat = new LinkedHashMap<>();
        for (Bill b : bills) {
            if (b.getCategoryId() == null) {
                continue;
            }
            byCat.merge(b.getCategoryId(), b.getAmount(), BigDecimal::add);
        }

        BigDecimal total = byCat.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        List<Map<String, Object>> result = byCat.entrySet().stream().map(entry -> {
            Category cat = categoryMapper.selectById(entry.getKey());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("categoryId", entry.getKey());
            m.put("categoryName", cat == null ? "未分类" : cat.getName());
            m.put("categoryIcon", cat == null ? "" : cat.getIcon());
            m.put("amount", entry.getValue());
            m.put("percent", percent(entry.getValue(), total));
            return m;
        }).collect(Collectors.toList());
        result.sort((a, b) -> ((BigDecimal) b.get("amount")).compareTo((BigDecimal) a.get("amount")));
        return result;
    }

    private BigDecimal percent(BigDecimal part, BigDecimal total) {
        if (total.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return part.multiply(BigDecimal.valueOf(100)).divide(total, 1, RoundingMode.HALF_UP);
    }
}
