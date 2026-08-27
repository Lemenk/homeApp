package com.familyhome.controller;

import com.familyhome.common.Result;
import com.familyhome.dto.BillQuery;
import com.familyhome.dto.BillVO;
import com.familyhome.dto.BillLogVO;
import com.familyhome.dto.BillRequest;
import com.familyhome.dto.PageResult;
import com.familyhome.security.UserContext;
import com.familyhome.service.BillService;
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

@RestController
@RequestMapping("/api")
public class BillController {

    private final BillService billService;

    public BillController(BillService billService) {
        this.billService = billService;
    }

    @GetMapping("/ledgers/{ledgerId}/bills")
    public Result<PageResult<BillVO>> list(@PathVariable Long ledgerId, BillQuery query) {
        return Result.ok(billService.listBills(UserContext.require(), ledgerId, query));
    }

    @PostMapping("/ledgers/{ledgerId}/bills")
    public Result<BillVO> create(@PathVariable Long ledgerId,
                                 @Valid @RequestBody BillRequest req) {
        req.setLedgerId(ledgerId);
        return Result.ok(billService.createBill(UserContext.require(), req));
    }

    @GetMapping("/bills/{id}")
    public Result<BillVO> get(@PathVariable Long id) {
        return Result.ok(billService.getBill(UserContext.require(), id));
    }

    @PutMapping("/bills/{id}")
    public Result<BillVO> update(@PathVariable Long id, @Valid @RequestBody BillRequest req) {
        return Result.ok(billService.updateBill(UserContext.require(), id, req));
    }

    @DeleteMapping("/bills/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        billService.deleteBill(UserContext.require(), id);
        return Result.ok();
    }

    @GetMapping("/bills/{id}/logs")
    public Result<List<BillLogVO>> logs(@PathVariable Long id) {
        return Result.ok(billService.billLogs(UserContext.require(), id));
    }
}
