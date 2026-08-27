package com.familyhome.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BillVO {
    private Long id;
    private Long ledgerId;
    private String type;
    private Long categoryId;
    private String categoryName;
    private String categoryIcon;
    private Long memberId;
    private String memberName;
    private BigDecimal amount;
    private LocalDateTime billDate;
    private String remark;
    private List<TagVO> tags;
    private List<BillAccountVO> accounts;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BillAccountVO {
        private Long accountId;
        private String accountName;
        private String direction;
        private BigDecimal amount;
        private Long pairId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TagVO {
        private Long id;
        private String name;
        private String color;
    }
}
