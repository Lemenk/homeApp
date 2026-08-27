package com.familyhome.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MemberVO {
    private Long userId;
    private String nickname;
    private String avatar;
    private String role;
    private LocalDateTime joinedAt;
}
