package com.familyhome.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 余额调整留痕实体：对应 t_account_balance_log 表，记录账户余额调整历史 */
@Data
@TableName("t_account_balance_log")
public class AccountBalanceLog {
    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 账户 ID */
    private Long accountId;
    /** 调整前余额 */
    private BigDecimal oldBalance;
    /** 调整后余额 */
    private BigDecimal newBalance;
    /** 调整原因 */
    private String reason;
    /** 操作人 ID */
    private Long operatorId;
    /** 创建时间 */
    private LocalDateTime createdAt;
}
