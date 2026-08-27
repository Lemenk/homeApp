package com.familyhome.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class BillQuery {
    private int page = 1;
    private int size = 20;
    private String type;
    private Long categoryId;
    private Long accountId;
    private Long memberId;
    private LocalDate startDate;
    private LocalDate endDate;
    private String keyword;
}
