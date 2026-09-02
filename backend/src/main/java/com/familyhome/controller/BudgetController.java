package com.familyhome.controller;

import com.familyhome.common.Result;
import com.familyhome.dto.CreateBudgetRequest;
import com.familyhome.security.UserContext;
import com.familyhome.service.BudgetService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** 预算接口：预算列表、新增、更新、删除 */
@RestController
@RequestMapping("/api")
public class BudgetController {

    private final BudgetService budgetService;

    public BudgetController(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    /** 查询账本预算列表（含已用/剩余金额） */
    @GetMapping("/ledgers/{ledgerId}/budgets")
    public Result<List<Map<String, Object>>> list(@PathVariable Long ledgerId) {
        return Result.ok(budgetService.list(UserContext.require(), ledgerId));
    }

    /** 新增预算（按分类设置月度或自定义周期额度） */
    @PostMapping("/ledgers/{ledgerId}/budgets")
    public Result<Map<String, Object>> create(@PathVariable Long ledgerId,
                                              @Valid @RequestBody CreateBudgetRequest req) {
        return Result.ok(budgetService.create(UserContext.require(), ledgerId, req));
    }

    /** 更新预算 */
    @PutMapping("/budgets/{id}")
    public Result<Map<String, Object>> update(@PathVariable Long id,
                                              @Valid @RequestBody CreateBudgetRequest req) {
        return Result.ok(budgetService.update(UserContext.require(), id, req));
    }

    /** 删除预算 */
    @DeleteMapping("/budgets/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        budgetService.delete(UserContext.require(), id);
        return Result.ok();
    }
}
