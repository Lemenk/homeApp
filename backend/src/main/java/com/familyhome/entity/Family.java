package com.familyhome.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 家庭实体：对应 t_family 表，一个家庭包含多名成员 */
@Data
@TableName("t_family")
public class Family {
    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 家庭名称 */
    private String name;
    /** 创建人（户主）用户 ID */
    private Long creatorId;
    /** 邀请码（成员凭此加入家庭） */
    private String inviteCode;
    /** 创建时间 */
    private LocalDateTime createdAt;
}
