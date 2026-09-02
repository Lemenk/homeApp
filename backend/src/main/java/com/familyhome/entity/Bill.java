package com.familyhome.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 账单实体：对应 t_bill 表，记录一笔支出/收入/转账流水 */
@Data
@TableName("t_bill")
public class Bill {
    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 所属账本 ID */
    private Long ledgerId;
    /** 账单类型：expense(支出)/income(收入)/transfer(转账) */
    private String type;
    /** 分类 ID */
    private Long categoryId;
    /** 记账人（家庭成员 ID） */
    private Long memberId;
    /** 账单金额 */
    private BigDecimal amount;
    /** 记账时间（秒级精度） */
    private LocalDateTime billDate;
    /** 备注 */
    private String remark;
    /** 创建人 ID */
    private Long createdBy;
    /** 创建时间 */
    private LocalDateTime createdAt;
    /** 更新时间 */
    private LocalDateTime updatedAt;
}
