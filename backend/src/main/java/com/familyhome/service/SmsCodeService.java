package com.familyhome.service;

/**
 * 短信验证码服务抽象。生产接入真实短信通道，开发/测试用内存实现。
 */
public interface SmsCodeService {

    /**
     * 发送验证码到手机号。
     */
    void sendCode(String phone);

    /**
     * 校验验证码，正确则消费（删除）。
     */
    boolean verifyCode(String phone, String code);
}
