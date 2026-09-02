package com.familyhome.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.familyhome.common.BizException;
import com.familyhome.dto.AccountVO;
import com.familyhome.dto.AdjustBalanceRequest;
import com.familyhome.dto.CreateAccountRequest;
import com.familyhome.dto.UpdateAccountRequest;
import com.familyhome.entity.Account;
import com.familyhome.entity.AccountBalanceLog;
import com.familyhome.mapper.AccountBalanceLogMapper;
import com.familyhome.mapper.AccountMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AccountService {

    private final AccountMapper accountMapper;
    private final AccountBalanceLogMapper balanceLogMapper;
    private final LedgerService ledgerService;

    public AccountService(AccountMapper accountMapper, AccountBalanceLogMapper balanceLogMapper,
                          LedgerService ledgerService) {
        this.accountMapper = accountMapper;
        this.balanceLogMapper = balanceLogMapper;
        this.ledgerService = ledgerService;
    }

    @Transactional
    public AccountVO create(Long userId, Long ledgerId, CreateAccountRequest req) {
        ledgerService.requireMember(ledgerId, userId);
        Account account = new Account();
        account.setLedgerId(ledgerId);
        account.setName(req.getName());
        account.setType(req.getType());
        account.setIcon(req.getIcon());
        account.setGroupName(req.getGroupName());
        account.setRemark(req.getRemark());
        account.setIncludeInTotal(req.getIncludeInTotal() == null ? 1 : req.getIncludeInTotal());
        account.setInitialBalance(req.getInitialBalance() == null ? BigDecimal.ZERO : req.getInitialBalance());
        account.setBalance(account.getInitialBalance());
        account.setStatus(1);
        account.setCreatedAt(LocalDateTime.now());
        accountMapper.insert(account);
        return toVO(account);
    }

    public List<Account> list(Long userId, Long ledgerId) {
        ledgerService.requireMember(ledgerId, userId);
        return accountMapper.selectList(
            Wrappers.<Account>lambdaQuery()
                .eq(Account::getLedgerId, ledgerId)
                .orderByAsc(Account::getType, Account::getId));
    }

    public Account getOwned(Long userId, Long accountId) {
        Account account = accountMapper.selectById(accountId);
        if (account == null) {
            throw BizException.notFound("账户不存在");
        }
        ledgerService.requireMember(account.getLedgerId(), userId);
        return account;
    }

    @Transactional
    public AccountVO adjustBalance(Long userId, Long accountId, AdjustBalanceRequest req) {
        Account account = getOwned(userId, accountId);
        BigDecimal oldBalance = account.getBalance();
        account.setBalance(req.getNewBalance());
        accountMapper.updateById(account);

        AccountBalanceLog log = new AccountBalanceLog();
        log.setAccountId(accountId);
        log.setOldBalance(oldBalance);
        log.setNewBalance(req.getNewBalance());
        log.setReason(req.getReason());
        log.setOperatorId(userId);
        log.setCreatedAt(LocalDateTime.now());
        balanceLogMapper.insert(log);
        return toVO(account);
    }

    /** 更新账户基本信息；余额发生变化时记录余额调整日志 */
    @Transactional
    public AccountVO update(Long userId, Long accountId, UpdateAccountRequest req) {
        Account account = getOwned(userId, accountId);
        account.setName(req.getName());
        if (req.getType() != null && !req.getType().isBlank()) {
            account.setType(req.getType());
        }
        // 分组/备注仅在有值时更新，避免未传时误清空已有数据
        if (req.getGroupName() != null) {
            account.setGroupName(req.getGroupName());
        }
        if (req.getRemark() != null) {
            account.setRemark(req.getRemark());
        }
        account.setIncludeInTotal(req.getIncludeInTotal() == null ? account.getIncludeInTotal() : req.getIncludeInTotal());
        if (req.getBalance() != null && req.getBalance().compareTo(account.getBalance()) != 0) {
            BigDecimal oldBalance = account.getBalance();
            account.setBalance(req.getBalance());
            AccountBalanceLog log = new AccountBalanceLog();
            log.setAccountId(accountId);
            log.setOldBalance(oldBalance);
            log.setNewBalance(req.getBalance());
            log.setReason("编辑账户修改余额");
            log.setOperatorId(userId);
            log.setCreatedAt(LocalDateTime.now());
            balanceLogMapper.insert(log);
        }
        accountMapper.updateById(account);
        return toVO(account);
    }

    /** 资产总览：按账户类型分组的余额 + 总资产/总负债/净资产 */
    public Map<String, Object> summary(Long userId, Long ledgerId) {
        ledgerService.requireMember(ledgerId, userId);
        List<Account> accounts = list(userId, ledgerId);
        Map<String, Object> result = new LinkedHashMap<>();
        BigDecimal totalAssets = BigDecimal.ZERO;
        BigDecimal totalLiability = BigDecimal.ZERO;
        for (Account a : accounts) {
            if ("credit".equals(a.getType())) {
                totalLiability = totalLiability.add(a.getBalance());
            } else if (a.getIncludeInTotal() == null || a.getIncludeInTotal() == 1) {
                totalAssets = totalAssets.add(a.getBalance());
            }
        }
        result.put("totalAssets", totalAssets);
        result.put("totalLiability", totalLiability);
        result.put("netAssets", totalAssets.subtract(totalLiability));
        result.put("accounts", accounts.stream().map(this::toVO).toList());
        return result;
    }

    private AccountVO toVO(Account a) {
        return new AccountVO(a.getId(), a.getLedgerId(), a.getType(), a.getName(),
            a.getIcon(), a.getInitialBalance(), a.getBalance(),
            a.getGroupName(), a.getRemark(), a.getIncludeInTotal());
    }
}
