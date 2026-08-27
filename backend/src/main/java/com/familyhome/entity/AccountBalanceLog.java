package com.familyhome.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_account_balance_log")
public class AccountBalanceLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long accountId;
    private BigDecimal oldBalance;
    private BigDecimal newBalance;
    private String reason;
    private Long operatorId;
    private LocalDateTime createdAt;
}
