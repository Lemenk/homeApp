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
 * Phase 4：记账核心验收。
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Rollback
class BillControllerTest extends ApiTestBase {

    private Long createPersonalLedger(String tk, String name) throws Exception {
        JsonNode node = objectMapper.readTree(mockMvc.perform(
                postJson("/api/ledgers", "{\"name\":\"" + name + "\",\"type\":\"personal\"}")
                    .header("Authorization", "Bearer " + tk))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        return node.path("data").path("id").asLong();
    }

    private Long createAccount(String tk, Long ledgerId, String name, String type, String balance) throws Exception {
        JsonNode node = objectMapper.readTree(mockMvc.perform(
                postJson("/api/ledgers/" + ledgerId + "/accounts",
                    "{\"name\":\"" + name + "\",\"type\":\"" + type + "\",\"initialBalance\":" + balance + "}")
                    .header("Authorization", "Bearer " + tk))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        return node.path("data").path("id").asLong();
    }

    private JsonNode getAccounts(String tk, Long ledgerId) throws Exception {
        return objectMapper.readTree(mockMvc.perform(
                get("/api/ledgers/" + ledgerId + "/accounts").header("Authorization", "Bearer " + tk))
            .andReturn().getResponse().getContentAsString()).path("data");
    }

    private java.math.BigDecimal balanceOf(JsonNode accounts, Long accountId) {
        for (JsonNode a : accounts) {
            if (a.path("id").asLong() == accountId) {
                return new java.math.BigDecimal(a.path("balance").asText());
            }
        }
        return java.math.BigDecimal.ZERO;
    }

    private Long firstExpenseCategory(String tk, Long ledgerId) throws Exception {
        JsonNode cats = objectMapper.readTree(mockMvc.perform(
                get("/api/ledgers/" + ledgerId + "/categories").header("Authorization", "Bearer " + tk))
            .andReturn().getResponse().getContentAsString()).path("data");
        for (JsonNode c : cats) {
            if ("expense".equals(c.path("type").asText())) {
                return c.path("id").asLong();
            }
        }
        throw new IllegalStateException("no expense category");
    }

    @Test
    void createExpense_withMultiAccount_splitUpdated() throws Exception {
        String tk = token("13800000040");
        Long ledgerId = createPersonalLedger(tk, "私账");
        Long a = createAccount(tk, ledgerId, "招商卡", "asset", "1000");
        Long b = createAccount(tk, ledgerId, "微信", "asset", "500");
        Long cat = firstExpenseCategory(tk, ledgerId);

        mockMvc.perform(postJson("/api/ledgers/" + ledgerId + "/bills",
                "{\"type\":\"expense\",\"categoryId\":" + cat + ",\"amount\":300,\"billDate\":\"2026-08-01T12:00:00\"," +
                "\"items\":[{\"accountId\":" + a + ",\"direction\":\"out\",\"amount\":200}," +
                "{\"accountId\":" + b + ",\"direction\":\"out\",\"amount\":100}]}")
                .header("Authorization", "Bearer " + tk))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.amount").value(300))
            .andExpect(jsonPath("$.data.accounts.length()").value(2));

        JsonNode accounts = getAccounts(tk, ledgerId);
        org.junit.jupiter.api.Assertions.assertEquals(0, balanceOf(accounts, a).compareTo(new java.math.BigDecimal("800")), "招商卡余额应减少200");
        org.junit.jupiter.api.Assertions.assertEquals(0, balanceOf(accounts, b).compareTo(new java.math.BigDecimal("400")), "微信余额应减少100");
    }

    @Test
    void createExpense_splitMismatch_rejected() throws Exception {
        String tk = token("13800000041");
        Long ledgerId = createPersonalLedger(tk, "私账");
        Long a = createAccount(tk, ledgerId, "卡A", "asset", "1000");
        Long cat = firstExpenseCategory(tk, ledgerId);

        mockMvc.perform(postJson("/api/ledgers/" + ledgerId + "/bills",
                "{\"type\":\"expense\",\"categoryId\":" + cat + ",\"amount\":300," +
                "\"items\":[{\"accountId\":" + a + ",\"direction\":\"out\",\"amount\":200}]}")
                .header("Authorization", "Bearer " + tk))
            .andExpect(status().isBadRequest());
    }

    @Test
    void createIncome_increasesBalance() throws Exception {
        String tk = token("13800000042");
        Long ledgerId = createPersonalLedger(tk, "私账");
        Long a = createAccount(tk, ledgerId, "工资卡", "asset", "0");
        Long cat = objectMapper.readTree(mockMvc.perform(
                get("/api/ledgers/" + ledgerId + "/categories").header("Authorization", "Bearer " + tk))
            .andReturn().getResponse().getContentAsString()).path("data").get(0).path("id").asLong();

        mockMvc.perform(postJson("/api/ledgers/" + ledgerId + "/bills",
                "{\"type\":\"income\",\"categoryId\":" + cat + ",\"amount\":500," +
                "\"items\":[{\"accountId\":" + a + ",\"direction\":\"in\",\"amount\":500}]}")
                .header("Authorization", "Bearer " + tk))
            .andExpect(status().isOk());

        JsonNode accounts = getAccounts(tk, ledgerId);
        org.junit.jupiter.api.Assertions.assertEquals(0, balanceOf(accounts, a).compareTo(new java.math.BigDecimal("500")));
    }

    @Test
    void createTransfer_bothSidesUpdated() throws Exception {
        String tk = token("13800000043");
        Long ledgerId = createPersonalLedger(tk, "私账");
        Long a = createAccount(tk, ledgerId, "卡A", "asset", "1000");
        Long b = createAccount(tk, ledgerId, "卡B", "asset", "500");

        mockMvc.perform(postJson("/api/ledgers/" + ledgerId + "/bills",
                "{\"type\":\"transfer\",\"amount\":200," +
                "\"items\":[{\"accountId\":" + a + ",\"direction\":\"out\",\"amount\":200}," +
                "{\"accountId\":" + b + ",\"direction\":\"in\",\"amount\":200}]}")
                .header("Authorization", "Bearer " + tk))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.type").value("transfer"));

        JsonNode accounts = getAccounts(tk, ledgerId);
        org.junit.jupiter.api.Assertions.assertEquals(0, balanceOf(accounts, a).compareTo(new java.math.BigDecimal("800")), "转出账户应减少");
        org.junit.jupiter.api.Assertions.assertEquals(0, balanceOf(accounts, b).compareTo(new java.math.BigDecimal("700")), "转入账户应增加");
    }

    @Test
    void createTransfer_sameAccountOutAndIn_rejected() throws Exception {
        String tk = token("13800000047");
        Long ledgerId = createPersonalLedger(tk, "私账");
        Long a = createAccount(tk, ledgerId, "卡A", "asset", "1000");

        // 同一账户同时作为转出与转入：余额对冲 = 白记，应被拒绝
        mockMvc.perform(postJson("/api/ledgers/" + ledgerId + "/bills",
                "{\"type\":\"transfer\",\"amount\":200," +
                "\"items\":[{\"accountId\":" + a + ",\"direction\":\"out\",\"amount\":200}," +
                "{\"accountId\":" + a + ",\"direction\":\"in\",\"amount\":200}]}")
                .header("Authorization", "Bearer " + tk))
            .andExpect(status().isBadRequest());

        // 余额不应变化
        JsonNode accounts = getAccounts(tk, ledgerId);
        org.junit.jupiter.api.Assertions.assertEquals(0, balanceOf(accounts, a).compareTo(new java.math.BigDecimal("1000")));
    }

    @Test
    void keywordSearch_onlyMatchesCurrentLedger() throws Exception {
        String tk = token("13800000048");
        Long ledgerA = createPersonalLedger(tk, "账本A");
        Long ledgerB = createPersonalLedger(tk, "账本B");
        Long accA = createAccount(tk, ledgerA, "卡", "asset", "100");
        Long accB = createAccount(tk, ledgerB, "卡", "asset", "100");
        Long catA = firstExpenseCategory(tk, ledgerA); // 默认首个支出分类为"餐饮"
        Long catB = firstExpenseCategory(tk, ledgerB);

        // 两个账本各记一笔，remark 不含关键字，仅靠分类名匹配
        JsonNode billA = objectMapper.readTree(mockMvc.perform(postJson("/api/ledgers/" + ledgerA + "/bills",
                "{\"type\":\"expense\",\"categoryId\":" + catA + ",\"amount\":10," +
                "\"items\":[{\"accountId\":" + accA + ",\"direction\":\"out\",\"amount\":10}]}")
                .header("Authorization", "Bearer " + tk))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).path("data");
        objectMapper.readTree(mockMvc.perform(postJson("/api/ledgers/" + ledgerB + "/bills",
                "{\"type\":\"expense\",\"categoryId\":" + catB + ",\"amount\":20," +
                "\"items\":[{\"accountId\":" + accB + ",\"direction\":\"out\",\"amount\":20}]}")
                .header("Authorization", "Bearer " + tk))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());

        // 在账本 A 搜索"餐饮"：只应命中账本 A 的账单，不含账本 B 的同名分类账单
        mockMvc.perform(get("/api/ledgers/" + ledgerA + "/bills?keyword=" + "餐饮")
                .header("Authorization", "Bearer " + tk))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.list.length()").value(1))
            .andExpect(jsonPath("$.data.list[0].id").value(billA.path("id").asLong()));
    }

    @Test
    void personalLedger_recordForOther_rejected() throws Exception {
        String tk = token("13800000044");
        String otherTk = token("13800000045");
        Long otherUserId = objectMapper.readTree(mockMvc.perform(
                get("/api/auth/me").header("Authorization", "Bearer " + otherTk))
            .andReturn().getResponse().getContentAsString()).path("data").path("id").asLong();
        Long ledgerId = createPersonalLedger(tk, "私账");
        Long a = createAccount(tk, ledgerId, "卡", "asset", "100");
        Long cat = firstExpenseCategory(tk, ledgerId);

        mockMvc.perform(postJson("/api/ledgers/" + ledgerId + "/bills",
                "{\"type\":\"expense\",\"categoryId\":" + cat + ",\"amount\":10,\"memberId\":" + otherUserId + "," +
                "\"items\":[{\"accountId\":" + a + ",\"direction\":\"out\",\"amount\":10}]}")
                .header("Authorization", "Bearer " + tk))
            .andExpect(status().isBadRequest());
    }

    @Test
    void publicLedger_recordForOtherMember_ok() throws Exception {
        String creatorTk = token("13800000046");
        mockMvc.perform(postJson("/api/families", "{\"name\":\"家\"}")
                .header("Authorization", "Bearer " + creatorTk)).andExpect(status().isOk());
        String memberTk = token("13800000047");
        String inviteCode = objectMapper.readTree(mockMvc.perform(
                get("/api/families/me").header("Authorization", "Bearer " + creatorTk))
            .andReturn().getResponse().getContentAsString()).path("data").path("inviteCode").asText();
        mockMvc.perform(postJson("/api/families/join", "{\"inviteCode\":\"" + inviteCode + "\"}")
                .header("Authorization", "Bearer " + memberTk)).andExpect(status().isOk());

        Long ledgerId = createPersonalLedger(creatorTk, "temp");
        // 创建公共账本
        JsonNode ledgerNode = objectMapper.readTree(mockMvc.perform(
                postJson("/api/ledgers", "{\"name\":\"家庭账本\",\"type\":\"public\"}")
                    .header("Authorization", "Bearer " + creatorTk))
            .andReturn().getResponse().getContentAsString());
        Long publicLedgerId = ledgerNode.path("data").path("id").asLong();
        org.junit.jupiter.api.Assertions.assertNotEquals(ledgerId, publicLedgerId);

        Long a = createAccount(creatorTk, publicLedgerId, "公共卡", "asset", "1000");
        Long cat = firstExpenseCategory(creatorTk, publicLedgerId);
        Long memberUserId = objectMapper.readTree(mockMvc.perform(
                get("/api/auth/me").header("Authorization", "Bearer " + memberTk))
            .andReturn().getResponse().getContentAsString()).path("data").path("id").asLong();

        mockMvc.perform(postJson("/api/ledgers/" + publicLedgerId + "/bills",
                "{\"type\":\"expense\",\"categoryId\":" + cat + ",\"amount\":66,\"memberId\":" + memberUserId + "," +
                "\"items\":[{\"accountId\":" + a + ",\"direction\":\"out\",\"amount\":66}]}")
                .header("Authorization", "Bearer " + creatorTk))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.memberId").value(memberUserId));
    }

    @Test
    void deleteBill_reversesBalance() throws Exception {
        String tk = token("13800000048");
        Long ledgerId = createPersonalLedger(tk, "私账");
        Long a = createAccount(tk, ledgerId, "卡", "asset", "1000");
        Long cat = firstExpenseCategory(tk, ledgerId);
        JsonNode bill = objectMapper.readTree(mockMvc.perform(
                postJson("/api/ledgers/" + ledgerId + "/bills",
                    "{\"type\":\"expense\",\"categoryId\":" + cat + ",\"amount\":200," +
                    "\"items\":[{\"accountId\":" + a + ",\"direction\":\"out\",\"amount\":200}]}")
                    .header("Authorization", "Bearer " + tk))
            .andReturn().getResponse().getContentAsString());
        Long billId = bill.path("data").path("id").asLong();
        org.junit.jupiter.api.Assertions.assertEquals(0, balanceOf(getAccounts(tk, ledgerId), a).compareTo(new java.math.BigDecimal("800")));

        mockMvc.perform(delete("/api/bills/" + billId).header("Authorization", "Bearer " + tk))
            .andExpect(status().isOk());
        org.junit.jupiter.api.Assertions.assertEquals(0, balanceOf(getAccounts(tk, ledgerId), a).compareTo(new java.math.BigDecimal("1000")), "删除后余额应回滚");
    }

    @Test
    void updateBill_adjustsBalance_andWritesAuditLog() throws Exception {
        String tk = token("13800000049");
        Long ledgerId = createPersonalLedger(tk, "私账");
        Long a = createAccount(tk, ledgerId, "卡", "asset", "1000");
        Long cat = firstExpenseCategory(tk, ledgerId);
        JsonNode bill = objectMapper.readTree(mockMvc.perform(
                postJson("/api/ledgers/" + ledgerId + "/bills",
                    "{\"type\":\"expense\",\"categoryId\":" + cat + ",\"amount\":100," +
                    "\"items\":[{\"accountId\":" + a + ",\"direction\":\"out\",\"amount\":100}]}")
                    .header("Authorization", "Bearer " + tk))
            .andReturn().getResponse().getContentAsString());
        Long billId = bill.path("data").path("id").asLong();
        org.junit.jupiter.api.Assertions.assertEquals(0, balanceOf(getAccounts(tk, ledgerId), a).compareTo(new java.math.BigDecimal("900")));

        mockMvc.perform(put("/api/bills/" + billId)
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("{\"type\":\"expense\",\"categoryId\":" + cat + ",\"amount\":50," +
                    "\"items\":[{\"accountId\":" + a + ",\"direction\":\"out\",\"amount\":50}]}")
                .header("Authorization", "Bearer " + tk))
            .andExpect(status().isOk());
        org.junit.jupiter.api.Assertions.assertEquals(0, balanceOf(getAccounts(tk, ledgerId), a).compareTo(new java.math.BigDecimal("950")), "修改后余额应重算");

        // 操作留痕：应有 create + update
        mockMvc.perform(get("/api/bills/" + billId + "/logs").header("Authorization", "Bearer " + tk))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    void liabilityAccount_expense_increasesDebt() throws Exception {
        String tk = token("13800000050");
        Long ledgerId = createPersonalLedger(tk, "私账");
        Long card = createAccount(tk, ledgerId, "信用卡", "credit", "0");
        Long cat = firstExpenseCategory(tk, ledgerId);

        mockMvc.perform(postJson("/api/ledgers/" + ledgerId + "/bills",
                "{\"type\":\"expense\",\"categoryId\":" + cat + ",\"amount\":300," +
                "\"items\":[{\"accountId\":" + card + ",\"direction\":\"out\",\"amount\":300}]}")
                .header("Authorization", "Bearer " + tk))
            .andExpect(status().isOk());

        // 负债账户余额（欠款）应增加
        org.junit.jupiter.api.Assertions.assertEquals(0, balanceOf(getAccounts(tk, ledgerId), card).compareTo(new java.math.BigDecimal("300")));
    }

    @Test
    void listBills_withFilters() throws Exception {
        String tk = token("13800000051");
        Long ledgerId = createPersonalLedger(tk, "私账");
        Long a = createAccount(tk, ledgerId, "卡", "asset", "1000");
        Long cat = firstExpenseCategory(tk, ledgerId);
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(postJson("/api/ledgers/" + ledgerId + "/bills",
                "{\"type\":\"expense\",\"categoryId\":" + cat + ",\"amount\":10,\"remark\":\"早餐" + i + "\"," +
                "\"items\":[{\"accountId\":" + a + ",\"direction\":\"out\",\"amount\":10}]}")
                .header("Authorization", "Bearer " + tk)).andExpect(status().isOk());
        }
        mockMvc.perform(get("/api/ledgers/" + ledgerId + "/bills?type=expense")
                .header("Authorization", "Bearer " + tk))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(3));
        mockMvc.perform(get("/api/ledgers/" + ledgerId + "/bills").queryParam("keyword", "早餐")
                .header("Authorization", "Bearer " + tk))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(3));
    }
}
