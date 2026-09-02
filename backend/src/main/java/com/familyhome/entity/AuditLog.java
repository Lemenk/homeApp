package com.familyhome.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 操作留痕实体：对应 t_audit_log 表，记录账单的创建/修改/删除等审计操作 */
@Data
@TableName("t_audit_log")
public class AuditLog {
    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 所属账本 ID */
    private Long ledgerId;
    /** 关联账单 ID */
    private Long billId;
    /** 操作人 ID */
    private Long operatorId;
    /** 操作类型：create/update/delete */
    private String action;
    /** 变更详情（JSON，含账单快照/修改前后对比） */
    private String changeDetail;
    /** 操作时间 */
    private LocalDateTime createdAt;
}
