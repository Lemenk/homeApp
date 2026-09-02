package com.familyhome.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountVO {
    private Long id;
    private Long ledgerId;
    private String type;
    private String name;
    private String icon;
    private BigDecimal balance;
    private String groupName;
    private String remark;
    private Integer includeInTotal;
}
