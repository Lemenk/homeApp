package com.familyhome.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("t_budget")
public class Budget {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long ledgerId;
    private Long categoryId;
    private String periodType;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal amount;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
