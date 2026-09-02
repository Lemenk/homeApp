package com.familyhome.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 分类实体：对应 t_category 表，定义账本内的支出/收入分类（如餐饮、交通、工资等） */
@Data
@TableName("t_category")
public class Category {
    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 所属账本 ID */
    private Long ledgerId;
    /** 分类类型：expense(支出)/income(收入) */
    private String type;
    /** 分类名称 */
    private String name;
    /** 图标 key（对应前端 CATEGORY_ICONS 映射表，如 food/traffic/salary 等） */
    private String icon;
    /** 排序值（越小越靠前） */
    private Integer sort;
    /** 是否启用：1=启用，0=停用 */
    private Integer enabled;
    /** 创建时间 */
    private LocalDateTime createdAt;
}
