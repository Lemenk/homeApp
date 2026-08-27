package com.familyhome.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateFamilyRequest {

    @NotBlank(message = "家庭名称不能为空")
    @Size(max = 64, message = "家庭名称过长")
    private String name;
}
