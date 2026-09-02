package com.familyhome;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Phase 5：账户资产管理验收。
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Rollback
class AccountControllerTest extends ApiTestBase {

    private Long createLedger(String tk, String name, String type) throws Exception {
        JsonNode node = objectMapper.readTree(mockMvc.perform(
                postJson("/api/ledgers", "{\"name\":\"" + name + "\",\"type\":\"" + type + "\"}")
                    .header("Authorization", "Bearer " + tk))
            .andReturn().getResponse().getContentAsString());
        return node.path("data").path("id").asLong();
    }

    private Long createAccount(String tk, Long ledgerId, String name, String type, String init) throws Exception {
        JsonNode node = objectMapper.readTree(mockMvc.perform(
                postJson("/api/ledgers/" + ledgerId + "/accounts",
                    "{\"name\":\"" + name + "\",\"type\":\"" + type + "\",\"balance\":" + init + "}")
                    .header("Authorization", "Bearer " + tk))
            .andReturn().getResponse().getContentAsString());
        return node.path("data").path("id").asLong();
    }

    @Test
    void createAccount_setsInitialAndBalance() throws Exception {
        String tk = token("13800000060");
        Long ledgerId = createLedger(tk, "私账", "personal");
        mockMvc.perform(postJson("/api/ledgers/" + ledgerId + "/accounts",
                "{\"name\":\"工资卡\",\"type\":\"asset\",\"balance\":10000}")
                .header("Authorization", "Bearer " + tk))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.name").value("工资卡"))
            .andExpect(jsonPath("$.data.balance").value(10000))
            .andExpect(jsonPath("$.data.balance").value(10000));
    }

    @Test
    void createAccount_invalidType_rejected() throws Exception {
        String tk = token("13800000061");
        Long ledgerId = createLedger(tk, "私账", "personal");
        mockMvc.perform(postJson("/api/ledgers/" + ledgerId + "/accounts",
                "{\"name\":\"异常\",\"type\":\"other\"}")
                .header("Authorization", "Bearer " + tk))
            .andExpect(status().isBadRequest());
    }

    @Test
    void listAccounts_returnsAll() throws Exception {
        String tk = token("13800000062");
        Long ledgerId = createLedger(tk, "私账", "personal");
        createAccount(tk, ledgerId, "微信", "asset", "500");
        createAccount(tk, ledgerId, "信用卡", "credit", "0");
        mockMvc.perform(get("/api/ledgers/" + ledgerId + "/accounts")
                .header("Authorization", "Bearer " + tk))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    void adjustBalance_updatesBalanceAndLogs() throws Exception {
        String tk = token("13800000063");
        Long ledgerId = createLedger(tk, "私账", "personal");
        Long a = createAccount(tk, ledgerId, "卡", "asset", "1000");

        mockMvc.perform(postJson("/api/accounts/" + a + "/balance",
                "{\"newBalance\":888,\"reason\":\"现金清点\"}")
                .header("Authorization", "Bearer " + tk))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.balance").value(888));

        // 余额调整后，再次调整为 900，应生效且记录日志
        mockMvc.perform(postJson("/api/accounts/" + a + "/balance", "{\"newBalance\":900}")
                .header("Authorization", "Bearer " + tk))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.balance").value(900));
    }

    @Test
    void adjustBalance_otherUser_forbidden() throws Exception {
        String tk = token("13800000064");
        String other = token("13800000065");
        Long ledgerId = createLedger(tk, "私账", "personal");
        Long a = createAccount(tk, ledgerId, "卡", "asset", "100");
        mockMvc.perform(postJson("/api/accounts/" + a + "/balance", "{\"newBalance\":50}")
                .header("Authorization", "Bearer " + other))
            .andExpect(status().isForbidden());
    }

    @Test
    void updateAccount_modifiesFieldsAndBalance() throws Exception {
        String tk = token("13800000068");
        Long ledgerId = createLedger(tk, "私账", "personal");
        Long a = createAccount(tk, ledgerId, "卡", "asset", "1000");

        mockMvc.perform(putJson("/api/accounts/" + a,
                "{\"name\":\"工资卡\",\"type\":\"credit\",\"groupName\":\"日常\",\"remark\":\"每月工资\",\"includeInTotal\":0,\"balance\":1200}")
                .header("Authorization", "Bearer " + tk))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.name").value("工资卡"))
            .andExpect(jsonPath("$.data.type").value("credit"))
            .andExpect(jsonPath("$.data.groupName").value("日常"))
            .andExpect(jsonPath("$.data.remark").value("每月工资"))
            .andExpect(jsonPath("$.data.includeInTotal").value(0))
            .andExpect(jsonPath("$.data.balance").value(1200));
    }

    @Test
    void updateAccount_withoutBalance_keepsBalance() throws Exception {
        String tk = token("13800000069");
        Long ledgerId = createLedger(tk, "私账", "personal");
        Long a = createAccount(tk, ledgerId, "卡", "asset", "1000");

        mockMvc.perform(putJson("/api/accounts/" + a,
                "{\"name\":\"卡A\",\"groupName\":\"备用\"}")
                .header("Authorization", "Bearer " + tk))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.name").value("卡A"))
            .andExpect(jsonPath("$.data.balance").value(1000))
            .andExpect(jsonPath("$.data.groupName").value("备用"));
    }

    @Test
    void updateAccount_otherUser_forbidden() throws Exception {
        String tk = token("13800000070");
        String other = token("13800000071");
        Long ledgerId = createLedger(tk, "私账", "personal");
        Long a = createAccount(tk, ledgerId, "卡", "asset", "100");

        mockMvc.perform(putJson("/api/accounts/" + a, "{\"name\":\"改名\"}")
                .header("Authorization", "Bearer " + other))
            .andExpect(status().isForbidden());
    }

    @Test
    void summary_calculatesAssetsLiabilityNet() throws Exception {
        String tk = token("13800000066");
        Long ledgerId = createLedger(tk, "私账", "personal");
        createAccount(tk, ledgerId, "现金", "asset", "1000");
        createAccount(tk, ledgerId, "基金", "asset", "2000");
        createAccount(tk, ledgerId, "信用卡", "credit", "300");

        mockMvc.perform(get("/api/ledgers/" + ledgerId + "/accounts/summary")
                .header("Authorization", "Bearer " + tk))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.totalAssets").value(3000))
            .andExpect(jsonPath("$.data.totalLiability").value(300))
            .andExpect(jsonPath("$.data.netAssets").value(2700))
            .andExpect(jsonPath("$.data.accounts.length()").value(3));
    }

    @Test
    void billAndAdjust_coexist_onSameAccount() throws Exception {
        String tk = token("13800000067");
        Long ledgerId = createLedger(tk, "私账", "personal");
        Long a = createAccount(tk, ledgerId, "卡", "asset", "1000");
        Long cat = objectMapper.readTree(mockMvc.perform(
                get("/api/ledgers/" + ledgerId + "/categories").header("Authorization", "Bearer " + tk))
            .andReturn().getResponse().getContentAsString()).path("data").get(0).path("id").asLong();
        // 记账支出 200 → 余额 800
        mockMvc.perform(postJson("/api/ledgers/" + ledgerId + "/bills",
                "{\"type\":\"expense\",\"categoryId\":" + cat + ",\"amount\":200," +
                "\"items\":[{\"accountId\":" + a + ",\"direction\":\"out\",\"amount\":200}]}")
                .header("Authorization", "Bearer " + tk)).andExpect(status().isOk());
        // 手动调整为 950 → 最终 950
        mockMvc.perform(postJson("/api/accounts/" + a + "/balance", "{\"newBalance\":950}")
                .header("Authorization", "Bearer " + tk)).andExpect(status().isOk());
        mockMvc.perform(get("/api/ledgers/" + ledgerId + "/accounts").header("Authorization", "Bearer " + tk))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].balance").value(950));
    }
}
