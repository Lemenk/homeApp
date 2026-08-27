package com.familyhome.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("t_bill_account")
public class BillAccount {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long billId;
    private Long accountId;
    private String direction;
    private BigDecimal amount;
    private Long pairId;
}
