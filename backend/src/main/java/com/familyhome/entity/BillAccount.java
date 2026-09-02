package com.familyhome.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

/** 账单-账户明细实体：对应 t_bill_account 表，记录一笔账单涉及的账户收支明细（支持多账户拆分/转账） */
@Data
@TableName("t_bill_account")
public class BillAccount {
    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 账单 ID */
    private Long billId;
    /** 账户 ID */
    private Long accountId;
    /** 资金方向：out(支出/转出)/in(收入/转入) */
    private String direction;
    /** 该账户明细金额 */
    private BigDecimal amount;
    /** 转账配对 ID：同一笔转账的转入/转出两条明细通过 pair_id 关联 */
    private Long pairId;
}
