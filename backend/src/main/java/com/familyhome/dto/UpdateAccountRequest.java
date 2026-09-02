package com.familyhome.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/** 更新账户请求：可修改名称、类型、余额、分组、备注、是否计入总资产 */
@Data
public class UpdateAccountRequest {

    @NotBlank(message = "账户名称不能为空")
    @Size(max = 64, message = "账户名称过长")
    private String name;

    /** 账户类型：asset=资金账户 / credit=信贷账户 / stored_value=储值账户 */
    @Pattern(regexp = "asset|credit|stored_value", message = "账户类型不合法")
    private String type;

    @Size(max = 32, message = "分组名称过长")
    private String groupName;

    @Size(max = 255, message = "备注过长")
    private String remark;

    /** 是否计入总资产，默认 1 */
    private Integer includeInTotal;

    /** 修改后的账户余额；为空表示不调整余额 */
    private BigDecimal balance;
}
