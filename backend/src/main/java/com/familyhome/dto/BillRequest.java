package com.familyhome.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class BillRequest {

    /** 由路径变量注入，不由请求体提供 */
    private Long ledgerId;

    @NotBlank(message = "账单类型不能为空")
    @Pattern(regexp = "expense|income|transfer", message = "账单类型不合法")
    private String type;

    private Long categoryId;

    /** 记账人：公共账本可代记，个人账本固定为本人 */
    private Long memberId;

    @NotNull(message = "金额不能为空")
    private BigDecimal amount;

    private LocalDateTime billDate;

    @Size(max = 255)
    private String remark;

    private List<Long> tagIds;

    @NotNull(message = "账户明细不能为空")
    @Size(min = 1, message = "至少需要一个账户")
    private List<BillAccountItem> items;
}
