package com.familyhome.dto;

import com.familyhome.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserVO {
    private Long id;
    private String phone;
    private String nickname;
    private String avatar;
    private String openid;

    public static UserVO from(User u) {
        return new UserVO(u.getId(), u.getPhone(), u.getNickname(), u.getAvatar(), u.getOpenid());
    }
}
