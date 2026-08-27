package com.familyhome.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CreateBudgetRequest {

    @NotNull(message = "分类不能为空")
    private Long categoryId;

    @NotNull(message = "周期类型不能为空")
    @Pattern(regexp = "monthly|custom", message = "周期类型只能是 monthly 或 custom")
    private String periodType;

    private LocalDate startDate;
    private LocalDate endDate;

    @NotNull(message = "预算金额不能为空")
    private BigDecimal amount;

    @Size(max = 64)
    private String remark;
}
