package com.familyhome.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 标签实体：对应 t_tag 表，账本内可复用的自定义标签（如：出差、家庭开支） */
@Data
@TableName("t_tag")
public class Tag {
    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 所属账本 ID */
    private Long ledgerId;
    /** 标签名称 */
    private String name;
    /** 标签颜色（十六进制色值） */
    private String color;
    /** 创建人用户 ID */
    private Long createdBy;
    /** 创建时间 */
    private LocalDateTime createdAt;
}
