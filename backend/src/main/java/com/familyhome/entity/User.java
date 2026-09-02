package com.familyhome.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 用户实体：对应 t_user 表，存储注册用户基本信息 */
@Data
@TableName("t_user")
public class User {
    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 微信小程序 openid（登录凭证） */
    private String openid;
    /** 手机号（短信验证码登录） */
    private String phone;
    /** 昵称 */
    private String nickname;
    /** 头像 URL */
    private String avatar;
    /** 状态：1=正常，0=删除 */
    private Integer status;
    /** 创建时间 */
    private LocalDateTime createdAt;
    /** 更新时间 */
    private LocalDateTime updatedAt;
}
