package com.familyhome.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AdjustBalanceRequest {

    @NotNull(message = "调整后余额不能为空")
    private BigDecimal newBalance;

    @Size(max = 255)
    private String reason;
}
