package com.familyhome.service;

import com.familyhome.common.BizException;
import com.familyhome.common.JwtUtil;
import com.familyhome.dto.LoginResponse;
import com.familyhome.dto.UserVO;
import com.familyhome.entity.User;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserService userService;
    private final SmsCodeService smsCodeService;
    private final JwtUtil jwtUtil;

    public AuthService(UserService userService, SmsCodeService smsCodeService, JwtUtil jwtUtil) {
        this.userService = userService;
        this.smsCodeService = smsCodeService;
        this.jwtUtil = jwtUtil;
    }

    public void sendCode(String phone) {
        smsCodeService.sendCode(phone);
    }

    public LoginResponse loginByPhone(String phone, String code) {
        if (!smsCodeService.verifyCode(phone, code)) {
            throw BizException.badRequest("验证码错误或已过期");
        }
        User user = userService.loginOrCreateByPhone(phone);
        String token = jwtUtil.generateToken(user.getId());
        return new LoginResponse(token, UserVO.from(user));
    }

    public UserVO me(Long userId) {
        return UserVO.from(userService.getById(userId));
    }
}
