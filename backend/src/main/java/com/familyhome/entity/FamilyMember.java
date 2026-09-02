package com.familyhome.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 家庭成员关系实体：对应 t_family_member 表，记录用户与家庭的从属关系及角色 */
@Data
@TableName("t_family_member")
public class FamilyMember {
    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 家庭 ID */
    private Long familyId;
    /** 用户 ID */
    private Long userId;
    /** 角色：creator(户主)/member(成员) */
    private String role;
    /** 加入时间 */
    private LocalDateTime joinedAt;
}
