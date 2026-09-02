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

/** 账单接口：账单分页查询、新增、详情、更新、删除、操作留痕 */
@RestController
@RequestMapping("/api")
public class BillController {

    private final BillService billService;

    public BillController(BillService billService) {
        this.billService = billService;
    }

    /** 分页查询账本账单（支持类型/分类/账户/时间范围/关键词等过滤） */
    @GetMapping("/ledgers/{ledgerId}/bills")
    public Result<PageResult<BillVO>> list(@PathVariable Long ledgerId, BillQuery query) {
        return Result.ok(billService.listBills(UserContext.require(), ledgerId, query));
    }

    /** 新增账单（支出/收入/转账，支持多账户拆分） */
    @PostMapping("/ledgers/{ledgerId}/bills")
    public Result<BillVO> create(@PathVariable Long ledgerId,
                                 @Valid @RequestBody BillRequest req) {
        req.setLedgerId(ledgerId);
        return Result.ok(billService.createBill(UserContext.require(), req));
    }

    /** 查询账单详情（含账户明细、分类、标签等） */
    @GetMapping("/bills/{id}")
    public Result<BillVO> get(@PathVariable Long id) {
        return Result.ok(billService.getBill(UserContext.require(), id));
    }

    /** 更新账单：重算涉及账户余额，并写入操作留痕 */
    @PutMapping("/bills/{id}")
    public Result<BillVO> update(@PathVariable Long id, @Valid @RequestBody BillRequest req) {
        return Result.ok(billService.updateBill(UserContext.require(), id, req));
    }

    /** 删除账单：回滚账户余额，并写入操作留痕 */
    @DeleteMapping("/bills/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        billService.deleteBill(UserContext.require(), id);
        return Result.ok();
    }

    /** 查询账单操作留痕（创建/更新/删除历史） */
    @GetMapping("/bills/{id}/logs")
    public Result<List<BillLogVO>> logs(@PathVariable Long id) {
        return Result.ok(billService.billLogs(UserContext.require(), id));
    }
}
