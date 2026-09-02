package com.familyhome.controller;

import com.familyhome.common.Result;
import com.familyhome.dto.AccountVO;
import com.familyhome.dto.AdjustBalanceRequest;
import com.familyhome.dto.CreateAccountRequest;
import com.familyhome.dto.UpdateAccountRequest;
import com.familyhome.entity.Account;
import com.familyhome.security.UserContext;
import com.familyhome.service.AccountService;
import jakarta.validation.Valid;
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
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/ledgers/{ledgerId}/accounts")
    public Result<List<Account>> list(@PathVariable Long ledgerId) {
        return Result.ok(accountService.list(UserContext.require(), ledgerId));
    }

    @GetMapping("/ledgers/{ledgerId}/accounts/summary")
    public Result<Map<String, Object>> summary(@PathVariable Long ledgerId) {
        return Result.ok(accountService.summary(UserContext.require(), ledgerId));
    }

    @PostMapping("/ledgers/{ledgerId}/accounts")
    public Result<AccountVO> create(@PathVariable Long ledgerId,
                                    @Valid @RequestBody CreateAccountRequest req) {
        return Result.ok(accountService.create(UserContext.require(), ledgerId, req));
    }

    @PostMapping("/accounts/{id}/balance")
    public Result<AccountVO> adjustBalance(@PathVariable Long id,
                                           @Valid @RequestBody AdjustBalanceRequest req) {
        return Result.ok(accountService.adjustBalance(UserContext.require(), id, req));
    }

    @PutMapping("/accounts/{id}")
    public Result<AccountVO> update(@PathVariable Long id,
                                    @Valid @RequestBody UpdateAccountRequest req) {
        return Result.ok(accountService.update(UserContext.require(), id, req));
    }
}
