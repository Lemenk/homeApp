package com.familyhome.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TagRequest {

    @NotBlank(message = "标签名称不能为空")
    @Size(max = 32, message = "标签名称过长")
    private String name;

    @Size(max = 16)
    private String color;
}
