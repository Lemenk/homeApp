package com.familyhome.service;

import com.familyhome.common.BizException;
import com.familyhome.common.JwtUtil;
import com.familyhome.dto.LoginResponse;
import com.familyhome.dto.UserVO;
import com.familyhome.entity.User;
import org.springframework.stereotype.Service;

/** 认证服务：验证码发送、手机号登录（含新用户自动建默认账本）、当前用户查询 */
@Service
public class AuthService {

    private final UserService userService;
    private final SmsCodeService smsCodeService;
    private final JwtUtil jwtUtil;
    private final LedgerService ledgerService;

    public AuthService(UserService userService, SmsCodeService smsCodeService,
                       JwtUtil jwtUtil, LedgerService ledgerService) {
        this.userService = userService;
        this.smsCodeService = smsCodeService;
        this.jwtUtil = jwtUtil;
        this.ledgerService = ledgerService;
    }

    /** 发送短信验证码 */
    public void sendCode(String phone) {
        smsCodeService.sendCode(phone);
    }

    /**
     * 手机号验证码登录。
     * 新用户自动注册，并创建默认"个人账本"；返回 JWT 令牌与用户信息。
     */
    public LoginResponse loginByPhone(String phone, String code) {
        if (!smsCodeService.verifyCode(phone, code)) {
            throw BizException.badRequest("验证码错误或已过期");
        }
        User user = userService.findByPhone(phone);
        boolean isNewUser = (user == null);
        if (isNewUser) {
            // 新用户注册：创建账号，并自动创建一个"个人账本"作为默认账本
            user = userService.createUser(phone);
            ledgerService.createDefaultLedgerForNewUser(user.getId());
        }
        String token = jwtUtil.generateToken(user.getId());
        return new LoginResponse(token, UserVO.from(user));
    }

    /** 查询当前登录用户信息 */
    public UserVO me(Long userId) {
        return UserVO.from(userService.getById(userId));
    }
}
