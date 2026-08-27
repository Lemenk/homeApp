package com.familyhome.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_category")
public class Category {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long ledgerId;
    private String type;
    private String name;
    private String icon;
    private Integer sort;
    private Integer enabled;
    private LocalDateTime createdAt;
}
