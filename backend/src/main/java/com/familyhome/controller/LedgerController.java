package com.familyhome.controller;

import com.familyhome.common.Result;
import com.familyhome.dto.CreateLedgerRequest;
import com.familyhome.dto.LedgerVO;
import com.familyhome.security.UserContext;
import com.familyhome.service.LedgerService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ledgers")
public class LedgerController {

    private final LedgerService ledgerService;

    public LedgerController(LedgerService ledgerService) {
        this.ledgerService = ledgerService;
    }

    @GetMapping
    public Result<List<LedgerVO>> list() {
        return Result.ok(ledgerService.listLedgers(UserContext.require()));
    }

    @PostMapping
    public Result<LedgerVO> create(@Valid @RequestBody CreateLedgerRequest req) {
        return Result.ok(ledgerService.createLedger(UserContext.require(), req));
    }

    @GetMapping("/{id}")
    public Result<LedgerVO> get(@PathVariable Long id) {
        return Result.ok(ledgerService.getLedger(UserContext.require(), id));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        ledgerService.deleteLedger(UserContext.require(), id);
        return Result.ok();
    }
}
