package com.familyhome.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class BillAccountItem {

    @NotNull(message = "账户不能为空")
    private Long accountId;

    @Pattern(regexp = "out|in", message = "资金方向只能是 out 或 in")
    private String direction;

    @NotNull(message = "账户金额不能为空")
    private BigDecimal amount;
}
