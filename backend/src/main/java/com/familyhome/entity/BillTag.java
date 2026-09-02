package com.familyhome.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/** 账单-标签关联实体：对应 t_bill_tag 表，账单与标签的多对多中间表 */
@Data
@TableName("t_bill_tag")
public class BillTag {
    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 账单 ID */
    private Long billId;
    /** 标签 ID */
    private Long tagId;
}
