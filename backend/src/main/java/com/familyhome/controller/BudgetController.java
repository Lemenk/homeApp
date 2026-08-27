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

@RestController
@RequestMapping("/api")
public class BudgetController {

    private final BudgetService budgetService;

    public BudgetController(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    @GetMapping("/ledgers/{ledgerId}/budgets")
    public Result<List<Map<String, Object>>> list(@PathVariable Long ledgerId) {
        return Result.ok(budgetService.list(UserContext.require(), ledgerId));
    }

    @PostMapping("/ledgers/{ledgerId}/budgets")
    public Result<Map<String, Object>> create(@PathVariable Long ledgerId,
                                              @Valid @RequestBody CreateBudgetRequest req) {
        return Result.ok(budgetService.create(UserContext.require(), ledgerId, req));
    }

    @PutMapping("/budgets/{id}")
    public Result<Map<String, Object>> update(@PathVariable Long id,
                                              @Valid @RequestBody CreateBudgetRequest req) {
        return Result.ok(budgetService.update(UserContext.require(), id, req));
    }

    @DeleteMapping("/budgets/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        budgetService.delete(UserContext.require(), id);
        return Result.ok();
    }
}
