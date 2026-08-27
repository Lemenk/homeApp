package com.familyhome.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_ledger_member")
public class LedgerMember {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long ledgerId;
    private Long userId;
    private String role;
    private LocalDateTime joinedAt;
}
