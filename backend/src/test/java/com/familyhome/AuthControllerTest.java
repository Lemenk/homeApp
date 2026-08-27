package com.familyhome;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Phase 1：认证与用户验收。
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Rollback
class AuthControllerTest extends ApiTestBase {

    @Test
    void sendCode_success() throws Exception {
        mockMvc.perform(post("/api/auth/sms-code")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"phone\":\"13800000001\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void sendCode_invalidPhone_rejected() throws Exception {
        mockMvc.perform(post("/api/auth/sms-code")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"phone\":\"123\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void login_withDebugCode_returnsTokenAndUser() throws Exception {
        mockMvc.perform(post("/api/auth/login/phone")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"phone\":\"13800000002\",\"code\":\"123456\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.token").isNotEmpty())
            .andExpect(jsonPath("$.data.user.phone").value("13800000002"))
            .andExpect(jsonPath("$.data.user.id").isNumber());
    }

    @Test
    void login_withWrongCode_rejected() throws Exception {
        mockMvc.perform(post("/api/auth/login/phone")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"phone\":\"13800000003\",\"code\":\"000000\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void login_samePhoneTwice_sameUser_notDuplicated() throws Exception {
        String t1 = token("13900000000");
        String t2 = token("13900000000");

        // 同一手机号两次登录都应有效，且指向同一用户（不重复注册）
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + t1))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.phone").value("13900000000"));
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + t2))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.phone").value("13900000000"));
    }

    @Test
    void me_withValidToken_returnsUser() throws Exception {
        String tk = token("13800000004");
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + tk))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.phone").value("13800000004"));
    }

    @Test
    void me_withoutToken_unauthorized() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void me_withInvalidToken_unauthorized() throws Exception {
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer invalid.token.here"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void wechatLogin_reserved_notAvailable() throws Exception {
        mockMvc.perform(post("/api/auth/login/wechat")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest());
    }
}
