package com.familyhome.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 账单操作留痕：供详情页展示，含操作人昵称与可读摘要 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BillLogVO {
    private Long id;
    private String action;        // create/update/delete
    private String operatorName;  // 操作人昵称
    private String summary;       // 可读摘要，如「支出 ¥30.00 · 微信 · 午餐」
    private LocalDateTime createdAt;
}
