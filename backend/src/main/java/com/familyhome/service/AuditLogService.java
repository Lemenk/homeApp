package com.familyhome.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.familyhome.entity.AuditLog;
import com.familyhome.mapper.AuditLogMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/** 操作留痕服务：记录账单的创建/更新/删除审计日志并支持查询 */
@Service
public class AuditLogService {

    private final AuditLogMapper auditLogMapper;

    public AuditLogService(AuditLogMapper auditLogMapper) {
        this.auditLogMapper = auditLogMapper;
    }

    /** 记录一条操作留痕 */
    public void record(Long ledgerId, Long billId, Long operatorId, String action, String changeDetail) {
        AuditLog log = new AuditLog();
        log.setLedgerId(ledgerId);
        log.setBillId(billId);
        log.setOperatorId(operatorId);
        log.setAction(action);
        log.setChangeDetail(changeDetail);
        log.setCreatedAt(LocalDateTime.now());
        auditLogMapper.insert(log);
    }

    /** 查询某账单的全部留痕（按时间正序） */
    public List<AuditLog> listForBill(Long billId) {
        return auditLogMapper.selectList(
            Wrappers.<AuditLog>lambdaQuery()
                .eq(AuditLog::getBillId, billId)
                .orderByAsc(AuditLog::getId));
    }
}
