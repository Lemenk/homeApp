package com.familyhome.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 账户实体：对应 t_account 表，存储各类资金/信贷/储值账户及其余额 */
@Data
@TableName("t_account")
public class Account {
    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 所属账本 ID */
    private Long ledgerId;
    /** 账户类型：asset(资金)/credit(信贷)/stored_value(储值) */
    private String type;
    /** 账户名称 */
    private String name;
    /** 图标 key（对应前端 AppIcon 映射表，如 cash/wechat/alipay 等） */
    private String icon;
    /** 当前余额 */
    private BigDecimal balance;
    /** 账户分组（自由文本，如：日常、备用） */
    private String groupName;
    /** 备注 */
    private String remark;
    /** 是否计入总资产：1=计入，0=不计入 */
    private Integer includeInTotal;
    /** 状态：1=正常，0=删除 */
    private Integer status;
    /** 创建时间 */
    private LocalDateTime createdAt;
}
