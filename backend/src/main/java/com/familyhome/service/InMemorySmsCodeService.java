package com.familyhome.service;

import com.familyhome.common.BizException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 开发/测试用内存验证码服务：
 * - 验证码 5 分钟有效；
 * - debug 模式下固定万能码 123456，便于联调；生成的验证码打印日志。
 * - 频控：同号两次发送最小间隔 cooldown-seconds（默认 60s），每日上限 daily-limit（默认 10 条），防短信接口被刷。
 */
@Slf4j
@Service
public class InMemorySmsCodeService implements SmsCodeService {

    public static final String DEBUG_CODE = "123456";
    private static final long TTL_MILLIS = 5 * 60_000L;
    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.BASIC_ISO_DATE;

    private final Map<String, CodeEntry> store = new ConcurrentHashMap<>();
    private final Map<String, RateEntry> rateStore = new ConcurrentHashMap<>();
    private final boolean debug;
    private final long cooldownMillis;
    private final int dailyLimit;

    public InMemorySmsCodeService(@Value("${app.sms.debug:true}") boolean debug,
                                  @Value("${app.sms.cooldown-seconds:60}") long cooldownSeconds,
                                  @Value("${app.sms.daily-limit:10}") int dailyLimit) {
        this.debug = debug;
        this.cooldownMillis = cooldownSeconds * 1000L;
        this.dailyLimit = dailyLimit;
    }

    @Override
    public void sendCode(String phone) {
        long now = System.currentTimeMillis();
        String day = LocalDate.now().format(DAY_FMT);
        rateStore.compute(phone, (k, old) -> {
            RateEntry e = (old == null || !day.equals(old.dayKey)) ? new RateEntry(now, day, 0) : old;
            // dailyCount>0 才检查间隔，避免首次发送被误判
            if (e.dailyCount > 0 && now - e.lastSentAt < cooldownMillis) {
                throw BizException.badRequest("发送过于频繁，请稍后再试");
            }
            if (e.dailyCount >= dailyLimit) {
                throw BizException.badRequest("今日短信发送次数已达上限");
            }
            return new RateEntry(now, day, e.dailyCount + 1);
        });

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

    private record RateEntry(long lastSentAt, String dayKey, int dailyCount) {
    }
}
