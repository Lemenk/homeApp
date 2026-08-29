package com.familyhome;

import com.familyhome.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 安全配置：CORS 白名单收敛、Swagger/H2 生产隔离。
 */
class SecurityConfigTest {

    // ---------- CORS 白名单（纯单元测试） ----------

    @Test
    void cors_parsesWhitelist_andNeverWildcard() {
        SecurityConfig cfg = new SecurityConfig(null, "http://a.com, http://b.com,http://a.com", true);
        UrlBasedCorsConfigurationSource src = (UrlBasedCorsConfigurationSource) cfg.corsConfigurationSource();
        CorsConfiguration cc = src.getCorsConfigurations().get("/**");
        assertTrue(cc.getAllowedOrigins().contains("http://a.com"));
        assertTrue(cc.getAllowedOrigins().contains("http://b.com"));
        assertFalse(cc.getAllowedOrigins().contains("*"), "不允许 * 与凭据同用");
        assertTrue(cc.getAllowCredentials());
        // 逗号分隔 + 去重
        assertEquals(2, cc.getAllowedOrigins().size());
    }

    @Test
    void cors_emptyWhitelist_allowsNothing() {
        SecurityConfig cfg = new SecurityConfig(null, "  ", true);
        UrlBasedCorsConfigurationSource src = (UrlBasedCorsConfigurationSource) cfg.corsConfigurationSource();
        CorsConfiguration cc = src.getCorsConfigurations().get("/**");
        assertTrue(cc.getAllowedOrigins().isEmpty(), "空白名单不应放行任何来源");
    }

    // ---------- Swagger/H2 生产隔离（集成测试） ----------

    @SpringBootTest
    @AutoConfigureMockMvc
    @TestPropertySource(properties = {
        "app.security.dev-tools-enabled=false",
        "app.cors.allowed-origins=http://trusted.example.com"
    })
    static class ProdSecurityTest {
        @Autowired
        private MockMvc mockMvc;

        @Test
        void swagger_blocked_whenDevToolsDisabled() throws Exception {
            mockMvc.perform(get("/swagger-ui.html")).andExpect(status().is4xxClientError());
            mockMvc.perform(get("/v3/api-docs")).andExpect(status().is4xxClientError());
        }

        @Test
        void h2Console_blocked_whenDevToolsDisabled() throws Exception {
            mockMvc.perform(get("/h2-console")).andExpect(status().is4xxClientError());
        }

        @Test
        void publicEndpoints_stillAccessible() throws Exception {
            mockMvc.perform(get("/api/health")).andExpect(status().isOk());
        }
    }
}
