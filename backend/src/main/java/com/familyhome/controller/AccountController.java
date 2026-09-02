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

/** 账户接口：账户列表、资产汇总、新增账户、调整余额、更新账户 */
@RestController
@RequestMapping("/api")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    /** 查询账本下的账户列表（按创建时间排序） */
    @GetMapping("/ledgers/{ledgerId}/accounts")
    public Result<List<Account>> list(@PathVariable Long ledgerId) {
        return Result.ok(accountService.list(UserContext.require(), ledgerId));
    }

    /** 查询账本资产汇总：总资产、总负债、净资产及账户明细 */
    @GetMapping("/ledgers/{ledgerId}/accounts/summary")
    public Result<Map<String, Object>> summary(@PathVariable Long ledgerId) {
        return Result.ok(accountService.summary(UserContext.require(), ledgerId));
    }

    /** 新增账户：选择类型并填写名称、图标、初始余额等信息 */
    @PostMapping("/ledgers/{ledgerId}/accounts")
    public Result<AccountVO> create(@PathVariable Long ledgerId,
                                    @Valid @RequestBody CreateAccountRequest req) {
        return Result.ok(accountService.create(UserContext.require(), ledgerId, req));
    }

    /** 调整账户余额：记录余额调整留痕（旧余额 → 新余额） */
    @PostMapping("/accounts/{id}/balance")
    public Result<AccountVO> adjustBalance(@PathVariable Long id,
                                           @Valid @RequestBody AdjustBalanceRequest req) {
        return Result.ok(accountService.adjustBalance(UserContext.require(), id, req));
    }

    /** 更新账户信息：名称、类型、分组、备注、是否计入总资产；余额变化时记录调整留痕 */
    @PutMapping("/accounts/{id}")
    public Result<AccountVO> update(@PathVariable Long id,
                                    @Valid @RequestBody UpdateAccountRequest req) {
        return Result.ok(accountService.update(UserContext.require(), id, req));
    }
}
