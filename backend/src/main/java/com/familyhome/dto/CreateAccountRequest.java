package com.familyhome.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateAccountRequest {

    @NotBlank(message = "账户名称不能为空")
    @Size(max = 64, message = "账户名称过长")
    private String name;

    @NotBlank(message = "账户类型不能为空")
    @Pattern(regexp = "asset|credit|stored_value", message = "账户类型不合法")
    private String type;

    @Size(max = 32)
    private String icon;

    @Size(max = 32, message = "分组名称过长")
    private String groupName;

    @Size(max = 255, message = "备注过长")
    private String remark;

    /** 是否计入总资产，默认 1 */
    private Integer includeInTotal;

    private BigDecimal initialBalance;
}
