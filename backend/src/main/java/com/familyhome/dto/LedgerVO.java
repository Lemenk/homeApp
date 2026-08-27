package com.familyhome.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LedgerVO {
    private Long id;
    private String name;
    private String type;
    private String icon;
    private String theme;
    private Long ownerId;
    private Long familyId;
    /** 当前用户在账本中的角色 */
    private String role;
    private Long memberCount;
}
