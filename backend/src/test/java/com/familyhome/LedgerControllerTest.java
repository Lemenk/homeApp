package com.familyhome;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Phase 3：账本与基础数据（账本 CRUD、公共/个人、分类、标签）验收。
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Rollback
class LedgerControllerTest extends ApiTestBase {

    private String createFamilyFor(String creatorPhone) throws Exception {
        String tk = token(creatorPhone);
        mockMvc.perform(postJson("/api/families", "{\"name\":\"测试之家\"}")
                .header("Authorization", "Bearer " + tk))
            .andExpect(status().isOk());
        return tk;
    }

    private Long createLedgerId(String tk, String type, String name) throws Exception {
        JsonNode node = objectMapper.readTree(mockMvc.perform(
                postJson("/api/ledgers", "{\"name\":\"" + name + "\",\"type\":\"" + type + "\"}")
                    .header("Authorization", "Bearer " + tk))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        return node.path("data").path("id").asLong();
    }

    @Test
    void createPersonalLedger_seedsDefaultCategories() throws Exception {
        String tk = token("13800000020");
        mockMvc.perform(postJson("/api/ledgers", "{\"name\":\"我的私账\",\"type\":\"personal\"}")
                .header("Authorization", "Bearer " + tk))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.type").value("personal"))
            .andExpect(jsonPath("$.data.role").value("creator"))
            .andExpect(jsonPath("$.data.memberCount").value(1));

        Long ledgerId = createLedgerId(tk, "personal", "我的私账2");
        mockMvc.perform(get("/api/ledgers/" + ledgerId + "/categories")
                .header("Authorization", "Bearer " + tk))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").isNumber());
        // 默认分类：至少含支出餐饮、收入工资
        String body = mockMvc.perform(get("/api/ledgers/" + ledgerId + "/categories")
                .header("Authorization", "Bearer " + tk))
            .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        JsonNode cats = objectMapper.readTree(body).path("data");
        boolean hasExpense = false, hasIncome = false, hasFood = false;
        for (JsonNode c : cats) {
            if ("expense".equals(c.path("type").asText())) hasExpense = true;
            if ("income".equals(c.path("type").asText())) hasIncome = true;
            if ("餐饮".equals(c.path("name").asText())) hasFood = true;
        }
        org.junit.jupiter.api.Assertions.assertTrue(hasExpense && hasIncome && hasFood, "应默认生成支出/收入分类，含餐饮");
    }

    @Test
    void createPublicLedger_withoutFamily_rejected() throws Exception {
        String tk = token("13800000021");
        mockMvc.perform(postJson("/api/ledgers", "{\"name\":\"公共账本\",\"type\":\"public\"}")
                .header("Authorization", "Bearer " + tk))
            .andExpect(status().isBadRequest());
    }

    @Test
    void createPublicLedger_familyMembersAllAdded() throws Exception {
        String creatorTk = createFamilyFor("13800000022");
        // 成员加入家庭
        String memberTk = token("13800000023");
        String inviteCode = objectMapper.readTree(
                mockMvc.perform(get("/api/families/me").header("Authorization", "Bearer " + creatorTk))
                    .andReturn().getResponse().getContentAsString())
            .path("data").path("inviteCode").asText();
        mockMvc.perform(postJson("/api/families/join", "{\"inviteCode\":\"" + inviteCode + "\"}")
                .header("Authorization", "Bearer " + memberTk))
            .andExpect(status().isOk());

        // 创建者建公共账本 → 家庭成员都加入
        Long ledgerId = createLedgerId(creatorTk, "public", "家庭账本");
        mockMvc.perform(get("/api/ledgers/" + ledgerId).header("Authorization", "Bearer " + creatorTk))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.type").value("public"))
            .andExpect(jsonPath("$.data.memberCount").value(2));
        // 成员也能看到该账本
        mockMvc.perform(get("/api/ledgers/" + ledgerId).header("Authorization", "Bearer " + memberTk))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.role").value("member"));
    }

    @Test
    void listLedgers_returnsMine() throws Exception {
        String tk = token("13800000024");
        createLedgerId(tk, "personal", "私账A");
        createLedgerId(tk, "personal", "私账B");
        mockMvc.perform(get("/api/ledgers").header("Authorization", "Bearer " + tk))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    void accessLedger_asNonMember_forbidden() throws Exception {
        String ownerTk = token("13800000025");
        Long ledgerId = createLedgerId(ownerTk, "personal", "我的私账");
        String strangerTk = token("13800000026");
        mockMvc.perform(get("/api/ledgers/" + ledgerId).header("Authorization", "Bearer " + strangerTk))
            .andExpect(status().isForbidden());
    }

    @Test
    void categoryPermission_nonCreator_forbidden() throws Exception {
        String ownerTk = createFamilyFor("13800000027");
        String memberTk = token("13800000028");
        String inviteCode = objectMapper.readTree(
                mockMvc.perform(get("/api/families/me").header("Authorization", "Bearer " + ownerTk))
                    .andReturn().getResponse().getContentAsString())
            .path("data").path("inviteCode").asText();
        mockMvc.perform(postJson("/api/families/join", "{\"inviteCode\":\"" + inviteCode + "\"}")
                .header("Authorization", "Bearer " + memberTk))
            .andExpect(status().isOk());
        Long ledgerId = createLedgerId(ownerTk, "public", "公共账本");

        // 普通成员不能建分类
        mockMvc.perform(postJson("/api/ledgers/" + ledgerId + "/categories",
                "{\"name\":\"宠物\",\"type\":\"expense\"}")
                .header("Authorization", "Bearer " + memberTk))
            .andExpect(status().isForbidden());
        // 创建者可以建分类
        mockMvc.perform(postJson("/api/ledgers/" + ledgerId + "/categories",
                "{\"name\":\"宠物\",\"type\":\"expense\",\"icon\":\"pet\"}")
                .header("Authorization", "Bearer " + ownerTk))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.name").value("宠物"));
    }

    @Test
    void categoryToggleAndDelete() throws Exception {
        String tk = token("13800000029");
        Long ledgerId = createLedgerId(tk, "personal", "私账");
        JsonNode node = objectMapper.readTree(mockMvc.perform(
                postJson("/api/ledgers/" + ledgerId + "/categories", "{\"name\":\"宠物\",\"type\":\"expense\"}")
                    .header("Authorization", "Bearer " + tk))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        Long catId = node.path("data").path("id").asLong();

        // 停用
        mockMvc.perform(put("/api/categories/" + catId + "/toggle?enabled=false")
                .header("Authorization", "Bearer " + tk))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.enabled").value(0));
        // 删除（软删）
        mockMvc.perform(delete("/api/categories/" + catId)
                .header("Authorization", "Bearer " + tk))
            .andExpect(status().isOk());
    }

    @Test
    void tagCrud() throws Exception {
        String tk = token("13800000030");
        Long ledgerId = createLedgerId(tk, "personal", "私账");
        // 建标签
        JsonNode node = objectMapper.readTree(mockMvc.perform(
                postJson("/api/ledgers/" + ledgerId + "/tags", "{\"name\":\"聚餐\",\"color\":\"#ff0000\"}")
                    .header("Authorization", "Bearer " + tk))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        Long tagId = node.path("data").path("id").asLong();
        // 改
        mockMvc.perform(put("/api/tags/" + tagId)
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("{\"name\":\"团建\",\"color\":\"#00ff00\"}")
                .header("Authorization", "Bearer " + tk))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.name").value("团建"));
        // 删
        mockMvc.perform(delete("/api/tags/" + tagId).header("Authorization", "Bearer " + tk))
            .andExpect(status().isOk());
    }

    @Test
    void deleteLedger_permissionAndEffect() throws Exception {
        String tk = token("13800000031");
        Long ledgerId = createLedgerId(tk, "personal", "要删的账本");
        // 非创建者不能删
        String strangerTk = token("13800000032");
        mockMvc.perform(delete("/api/ledgers/" + ledgerId).header("Authorization", "Bearer " + strangerTk))
            .andExpect(status().isForbidden());
        // 创建者删除
        mockMvc.perform(delete("/api/ledgers/" + ledgerId).header("Authorization", "Bearer " + tk))
            .andExpect(status().isOk());
        // 列表不再包含
        mockMvc.perform(get("/api/ledgers").header("Authorization", "Bearer " + tk))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(0));
    }

    private Long userIdOf(String tk) throws Exception {
        return objectMapper.readTree(mockMvc.perform(
                get("/api/auth/me").header("Authorization", "Bearer " + tk))
            .andReturn().getResponse().getContentAsString()).path("data").path("id").asLong();
    }

    @Test
    void addMember_publicLedger_ok_thenRemove() throws Exception {
        String creatorTk = createFamilyFor("13800000033");
        Long ledgerId = createLedgerId(creatorTk, "public", "家庭账本");
        Long creatorId = userIdOf(creatorTk);

        // 外部用户（非家庭成员）
        String outsiderTk = token("13800000034");
        Long outsiderId = userIdOf(outsiderTk);
        // 添加前不可访问
        mockMvc.perform(get("/api/ledgers/" + ledgerId).header("Authorization", "Bearer " + outsiderTk))
            .andExpect(status().isForbidden());

        // 创建者添加成员
        mockMvc.perform(postJson("/api/ledgers/" + ledgerId + "/members", "{\"userId\":" + outsiderId + "}")
                .header("Authorization", "Bearer " + creatorTk))
            .andExpect(status().isOk());
        mockMvc.perform(get("/api/ledgers/" + ledgerId).header("Authorization", "Bearer " + creatorTk))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.memberCount").value(2));
        mockMvc.perform(get("/api/ledgers/" + ledgerId).header("Authorization", "Bearer " + outsiderTk))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.role").value("member"));

        // 重复添加拒绝
        mockMvc.perform(postJson("/api/ledgers/" + ledgerId + "/members", "{\"userId\":" + outsiderId + "}")
                .header("Authorization", "Bearer " + creatorTk))
            .andExpect(status().isBadRequest());

        // 移除成员后不可再访问
        mockMvc.perform(delete("/api/ledgers/" + ledgerId + "/members/" + outsiderId)
                .header("Authorization", "Bearer " + creatorTk))
            .andExpect(status().isOk());
        mockMvc.perform(get("/api/ledgers/" + ledgerId).header("Authorization", "Bearer " + outsiderTk))
            .andExpect(status().isForbidden());

        // 不能移除创建者本人
        mockMvc.perform(delete("/api/ledgers/" + ledgerId + "/members/" + creatorId)
                .header("Authorization", "Bearer " + creatorTk))
            .andExpect(status().isBadRequest());
    }

    @Test
    void addMember_personalLedger_rejected() throws Exception {
        String tk = token("13800000035");
        Long ledgerId = createLedgerId(tk, "personal", "私账");
        Long otherId = userIdOf(token("13800000036"));
        mockMvc.perform(postJson("/api/ledgers/" + ledgerId + "/members", "{\"userId\":" + otherId + "}")
                .header("Authorization", "Bearer " + tk))
            .andExpect(status().isBadRequest());
    }

    @Test
    void addMember_byNonMember_forbidden() throws Exception {
        String creatorTk = createFamilyFor("13800000037");
        Long ledgerId = createLedgerId(creatorTk, "public", "家庭账本");
        Long outsiderId = userIdOf(token("13800000038"));
        // 非账本成员/创建者无权添加
        mockMvc.perform(postJson("/api/ledgers/" + ledgerId + "/members", "{\"userId\":" + outsiderId + "}")
                .header("Authorization", "Bearer " + token("13800000039")))
            .andExpect(status().isForbidden());
    }
}
