package com.familyhome.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CategoryRequest {

    @NotBlank(message = "分类名称不能为空")
    @Size(max = 32, message = "分类名称过长")
    private String name;

    @NotBlank(message = "分类类型不能为空")
    @Pattern(regexp = "expense|income", message = "分类类型只能是 expense 或 income")
    private String type;

    @Size(max = 32)
    private String icon;

    private Integer sort;
}
