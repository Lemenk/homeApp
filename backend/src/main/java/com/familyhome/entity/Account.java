package com.familyhome.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_account")
public class Account {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long ledgerId;
    private String type;
    private String name;
    private String icon;
    private BigDecimal initialBalance;
    private BigDecimal balance;
    private String groupName;
    private String remark;
    private Integer includeInTotal;
    private Integer status;
    private LocalDateTime createdAt;
}
