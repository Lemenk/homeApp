package com.familyhome.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 账本实体：对应 t_ledger 表，个人账本或家庭公共账本，记录收支分类的归属单位 */
@Data
@TableName("t_ledger")
public class Ledger {
    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 账本名称 */
    private String name;
    /** 账本类型：public(公共)/personal(个人) */
    private String type;
    /** 图标 key */
    private String icon;
    /** 主题 */
    private String theme;
    /** 所有者用户 ID */
    private Long ownerId;
    /** 公共账本关联的家庭 ID */
    private Long familyId;
    /** 状态：1=正常，0=删除 */
    private Integer status;
    /** 是否为默认账本：1=默认，0=非默认（默认账本数据在首页优先展示） */
    private Integer isDefault;
    /** 创建时间 */
    private LocalDateTime createdAt;
}
