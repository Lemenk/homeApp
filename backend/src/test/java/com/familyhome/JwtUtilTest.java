package com.familyhome;

import com.familyhome.common.JwtUtil;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JWT 密钥安全：密钥强度校验（≥32 字节）与签发/解析正确性。
 */
class JwtUtilTest {

    private String base64Of(int bytes) {
        return Base64.getEncoder().encodeToString(new byte[bytes]);
    }

    @Test
    void shortSecret_rejectedAtStartup() {
        // 16 字节密钥（128 位）低于要求，构造时应抛异常
        IllegalStateException ex = assertThrows(IllegalStateException.class,
            () -> new JwtUtil(base64Of(16), 72));
        assertTrue(ex.getMessage().contains("32"));
    }

    @Test
    void validSecret_generatesAndParsesToken() {
        JwtUtil jwt = new JwtUtil(base64Of(32), 72);
        String token = jwt.generateToken(42L);
        assertEquals(42L, jwt.parseUserId(token));
    }

    @Test
    void invalidToken_returnsNull() {
        JwtUtil jwt = new JwtUtil(base64Of(32), 72);
        assertNull(jwt.parseUserId("not-a-jwt"));
        assertNull(jwt.parseUserId(""));
    }

    @Test
    void differentSecret_cannotParse() {
        // 内容不同的两个 ≥32 字节密钥，彼此不能解析
        byte[] a = new byte[32];
        byte[] b = new byte[32];
        b[0] = 1;
        b[1] = 2;
        JwtUtil issuer = new JwtUtil(Base64.getEncoder().encodeToString(a), 72);
        JwtUtil verifier = new JwtUtil(Base64.getEncoder().encodeToString(b), 72);
        assertNull(verifier.parseUserId(issuer.generateToken(1L)));
    }
}
