package com.familyhome.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 账本成员关系实体：对应 t_ledger_member 表，记录用户与账本的从属关系及角色 */
@Data
@TableName("t_ledger_member")
public class LedgerMember {
    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 账本 ID */
    private Long ledgerId;
    /** 用户 ID */
    private Long userId;
    /** 角色：owner(所有者)/member(成员) */
    private String role;
    /** 加入时间 */
    private LocalDateTime joinedAt;
}
