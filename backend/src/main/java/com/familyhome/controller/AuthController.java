package com.familyhome.controller;

import com.familyhome.common.BizException;
import com.familyhome.common.Result;
import com.familyhome.dto.LoginRequest;
import com.familyhome.dto.LoginResponse;
import com.familyhome.dto.SendCodeRequest;
import com.familyhome.dto.UserVO;
import com.familyhome.security.UserContext;
import com.familyhome.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 认证接口：短信验证码发送、手机号登录、微信登录预留、当前用户信息 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /** 发送短信验证码到指定手机号 */
    @PostMapping("/sms-code")
    public Result<Void> sendCode(@Valid @RequestBody SendCodeRequest req) {
        authService.sendCode(req.getPhone());
        return Result.ok();
    }

    /** 手机号 + 验证码登录，成功返回 JWT 令牌与用户信息 */
    @PostMapping("/login/phone")
    public Result<LoginResponse> loginByPhone(@Valid @RequestBody LoginRequest req) {
        return Result.ok(authService.loginByPhone(req.getPhone(), req.getCode()));
    }

    /**
     * 微信登录接口预留：待备案域名 + 企业主体资质具备后接入。
     */
    @PostMapping("/login/wechat")
    public Result<Void> loginByWechat() {
        throw BizException.badRequest("微信登录暂未开放，请使用手机号验证码登录");
    }

    /** 查询当前登录用户信息 */
    @GetMapping("/me")
    public Result<UserVO> me() {
        return Result.ok(authService.me(UserContext.require()));
    }
}
