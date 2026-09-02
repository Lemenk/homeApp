package com.familyhome.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 预算实体：对应 t_budget 表，记录某账本下分类的月度或自定义周期预算 */
@Data
@TableName("t_budget")
public class Budget {
    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 所属账本 ID */
    private Long ledgerId;
    /** 分类 ID */
    private Long categoryId;
    /** 周期类型：monthly(按月)/custom(自定义区间) */
    private String periodType;
    /** 自定义周期开始日期 */
    private LocalDate startDate;
    /** 自定义周期结束日期 */
    private LocalDate endDate;
    /** 预算金额 */
    private BigDecimal amount;
    /** 备注 */
    private String remark;
    /** 创建时间 */
    private LocalDateTime createdAt;
    /** 更新时间 */
    private LocalDateTime updatedAt;
}
