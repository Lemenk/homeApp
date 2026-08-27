package com.familyhome.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.familyhome.entity.AuditLog;
import com.familyhome.mapper.AuditLogMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuditLogService {

    private final AuditLogMapper auditLogMapper;

    public AuditLogService(AuditLogMapper auditLogMapper) {
        this.auditLogMapper = auditLogMapper;
    }

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

    public List<AuditLog> listForBill(Long billId) {
        return auditLogMapper.selectList(
            Wrappers.<AuditLog>lambdaQuery()
                .eq(AuditLog::getBillId, billId)
                .orderByAsc(AuditLog::getId));
    }
}
