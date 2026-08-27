package com.familyhome;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Phase 2：家庭与成员验收。
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Rollback
class FamilyControllerTest extends ApiTestBase {

    private String createFamily(String phone, String familyName) throws Exception {
        String tk = token(phone);
        mockMvc.perform(postJson("/api/families", "{\"name\":\"" + familyName + "\"}")
                .header("Authorization", "Bearer " + tk))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.role").value("creator"))
            .andExpect(jsonPath("$.data.inviteCode").isNotEmpty());
        return tk;
    }

    @Test
    void createFamily_success() throws Exception {
        String tk = token("13800000010");
        mockMvc.perform(postJson("/api/families", "{\"name\":\"我们一家\"}")
                .header("Authorization", "Bearer " + tk))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.name").value("我们一家"))
            .andExpect(jsonPath("$.data.role").value("creator"))
            .andExpect(jsonPath("$.data.inviteCode").isNotEmpty())
            .andExpect(jsonPath("$.data.members.length()").value(1));
    }

    @Test
    void createFamily_secondFamily_rejected() throws Exception {
        String tk = createFamily("13800000011", "家庭A");
        mockMvc.perform(postJson("/api/families", "{\"name\":\"家庭B\"}")
                .header("Authorization", "Bearer " + tk))
            .andExpect(status().isConflict());
    }

    @Test
    void joinByCode_memberAdded() throws Exception {
        String creatorTk = createFamily("13800000012", "幸福家庭");
        // 取邀请码
        String inviteCode = objectMapper.readTree(
                mockMvc.perform(get("/api/families/me").header("Authorization", "Bearer " + creatorTk))
                    .andReturn().getResponse().getContentAsString())
            .path("data").path("inviteCode").asText();

        // 成员通过邀请码加入
        String memberTk = token("13800000013");
        mockMvc.perform(postJson("/api/families/join", "{\"inviteCode\":\"" + inviteCode + "\"}")
                .header("Authorization", "Bearer " + memberTk))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.role").value("member"));

        // 创建者视角看到 2 个成员
        mockMvc.perform(get("/api/families/me").header("Authorization", "Bearer " + creatorTk))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.members.length()").value(2));
        // 成员视角同样看到同一家庭 2 个成员
        mockMvc.perform(get("/api/families/me").header("Authorization", "Bearer " + memberTk))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.members.length()").value(2));
    }

    @Test
    void joinByCode_invalidCode_rejected() throws Exception {
        String tk = token("13800000014");
        mockMvc.perform(postJson("/api/families/join", "{\"inviteCode\":\"BADCODE\"}")
                .header("Authorization", "Bearer " + tk))
            .andExpect(status().isNotFound());
    }

    @Test
    void inviteByNonCreator_forbidden() throws Exception {
        String creatorTk = createFamily("13800000015", "家");
        String inviteCode = objectMapper.readTree(
                mockMvc.perform(get("/api/families/me").header("Authorization", "Bearer " + creatorTk))
                    .andReturn().getResponse().getContentAsString())
            .path("data").path("inviteCode").asText();

        String memberTk = token("13800000016");
        mockMvc.perform(postJson("/api/families/join", "{\"inviteCode\":\"" + inviteCode + "\"}")
                .header("Authorization", "Bearer " + memberTk))
            .andExpect(status().isOk());

        // 普通成员尝试刷新邀请码 → 403
        mockMvc.perform(post("/api/families/1/invite").header("Authorization", "Bearer " + memberTk))
            .andExpect(status().isForbidden());
    }

    @Test
    void me_withoutFamily_returnsNull() throws Exception {
        String tk = token("13800000017");
        mockMvc.perform(get("/api/families/me").header("Authorization", "Bearer " + tk))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void memberList_accessibleToAnyMember() throws Exception {
        String creatorTk = createFamily("13800000018", "我的家");
        String inviteCode = objectMapper.readTree(
                mockMvc.perform(get("/api/families/me").header("Authorization", "Bearer " + creatorTk))
                    .andReturn().getResponse().getContentAsString())
            .path("data").path("inviteCode").asText();
        String memberTk = token("13800000019");
        mockMvc.perform(postJson("/api/families/join", "{\"inviteCode\":\"" + inviteCode + "\"}")
                .header("Authorization", "Bearer " + memberTk))
            .andExpect(status().isOk());

        Long familyId = objectMapper.readTree(
                mockMvc.perform(get("/api/families/me").header("Authorization", "Bearer " + creatorTk))
                    .andReturn().getResponse().getContentAsString())
            .path("data").path("id").asLong();

        mockMvc.perform(get("/api/families/" + familyId + "/members")
                .header("Authorization", "Bearer " + memberTk))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(2))
            .andExpect(jsonPath("$.data[0].role").value("creator"));
    }
}
