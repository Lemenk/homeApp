package com.familyhome.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 开发/测试用内存验证码服务：
 * - 验证码 5 分钟有效；
 * - debug 模式下固定万能码 123456，便于联调；生成的验证码打印日志。
 */
@Slf4j
@Service
public class InMemorySmsCodeService implements SmsCodeService {

    public static final String DEBUG_CODE = "123456";
    private static final long TTL_MILLIS = 5 * 60_000L;

    private final Map<String, CodeEntry> store = new ConcurrentHashMap<>();
    private final boolean debug;

    public InMemorySmsCodeService(@Value("${app.sms.debug:true}") boolean debug) {
        this.debug = debug;
    }

    @Override
    public void sendCode(String phone) {
        String code = String.format("%06d", new SecureRandom().nextInt(1_000_000));
        store.put(phone, new CodeEntry(code, System.currentTimeMillis() + TTL_MILLIS));
        log.info("[SMS-DEBUG] 验证码 {} 发送至手机号 {}", code, phone);
    }

    @Override
    public boolean verifyCode(String phone, String code) {
        if (code == null || code.isBlank()) {
            return false;
        }
        if (debug && DEBUG_CODE.equals(code)) {
            return true;
        }
        CodeEntry entry = store.get(phone);
        if (entry == null || entry.expireAt < System.currentTimeMillis()) {
            return false;
        }
        if (!entry.code.equals(code)) {
            return false;
        }
        store.remove(phone);
        return true;
    }

    private record CodeEntry(String code, long expireAt) {
    }
}
