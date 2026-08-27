package com.familyhome.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FamilyVO {
    private Long id;
    private String name;
    private Long creatorId;
    private String inviteCode;
    /** 当前用户在该家庭中的角色：creator / member */
    private String role;
    private List<MemberVO> members;
}
