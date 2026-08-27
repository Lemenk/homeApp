package com.familyhome.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("t_bill_tag")
public class BillTag {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long billId;
    private Long tagId;
}
