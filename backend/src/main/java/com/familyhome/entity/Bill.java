package com.familyhome.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_bill")
public class Bill {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long ledgerId;
    private String type;
    private Long categoryId;
    private Long memberId;
    private BigDecimal amount;
    private LocalDateTime billDate;
    private String remark;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
