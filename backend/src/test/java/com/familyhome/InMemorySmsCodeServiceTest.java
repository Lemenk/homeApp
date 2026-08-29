package com.familyhome;

import com.familyhome.common.BizException;
import com.familyhome.service.InMemorySmsCodeService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 短信发送频控与验证码校验。
 */
class InMemorySmsCodeServiceTest {

    @Test
    void sendCode_cooldown_blocksRapidResend() {
        InMemorySmsCodeService svc = new InMemorySmsCodeService(true, 60, 10);
        svc.sendCode("13800000000");
        // 60 秒内再次发送应被拒
        assertThrows(BizException.class, () -> svc.sendCode("13800000000"));
    }

    @Test
    void sendCode_dailyLimit_exceeded() {
        // cooldown=0 便于连续发送，每日上限 2
        InMemorySmsCodeService svc = new InMemorySmsCodeService(true, 0, 2);
        svc.sendCode("13800000001");
        svc.sendCode("13800000001");
        assertThrows(BizException.class, () -> svc.sendCode("13800000001"));
    }

    @Test
    void sendCode_differentPhone_isolated() {
        InMemorySmsCodeService svc = new InMemorySmsCodeService(true, 60, 10);
        svc.sendCode("13800000010");
        // 不同手机号不受同号频控影响
        svc.sendCode("13800000011");
    }

    @Test
    void verifyCode_masterCode_inDebugMode() {
        InMemorySmsCodeService svc = new InMemorySmsCodeService(true, 0, 10);
        assertTrue(svc.verifyCode("13800000020", "123456"));
    }

    @Test
    void verifyCode_rejectsInvalidOrMissing() {
        InMemorySmsCodeService svc = new InMemorySmsCodeService(false, 0, 10);
        assertFalse(svc.verifyCode("13800000021", "123456"), "非 debug 模式固定码不应通过");
        assertFalse(svc.verifyCode("13800000021", null));
        assertFalse(svc.verifyCode("13800000021", " "));
    }
}
