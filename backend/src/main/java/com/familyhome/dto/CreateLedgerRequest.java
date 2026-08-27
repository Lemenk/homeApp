package com.familyhome.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateLedgerRequest {

    @NotBlank(message = "账本名称不能为空")
    @Size(max = 64, message = "账本名称过长")
    private String name;

    @NotBlank(message = "账本类型不能为空")
    @Pattern(regexp = "public|personal", message = "账本类型只能是 public 或 personal")
    private String type;

    @Size(max = 32)
    private String icon;

    @Size(max = 32)
    private String theme;
}
