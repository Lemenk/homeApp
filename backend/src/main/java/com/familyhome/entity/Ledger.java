package com.familyhome.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_ledger")
public class Ledger {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String type;
    private String icon;
    private String theme;
    private Long ownerId;
    private Long familyId;
    private Integer status;
    /** 是否为默认账本：1=默认，0=非默认 */
    private Integer isDefault;
    private LocalDateTime createdAt;
}
