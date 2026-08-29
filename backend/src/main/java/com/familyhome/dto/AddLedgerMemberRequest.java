package com.familyhome.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AddLedgerMemberRequest {

    @NotNull(message = "用户 id 不能为空")
    private Long userId;
}
