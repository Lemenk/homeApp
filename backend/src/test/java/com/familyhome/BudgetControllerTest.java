package com.familyhome;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Phase 6：预算验收（按分类设置预算、周期自定义、超支标记）。
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Rollback
class BudgetControllerTest extends ApiTestBase {

    private Long createLedger(String tk, String name) throws Exception {
        JsonNode node = objectMapper.readTree(mockMvc.perform(
                postJson("/api/ledgers", "{\"name\":\"" + name + "\",\"type\":\"personal\"}")
                    .header("Authorization", "Bearer " + tk))
            .andReturn().getResponse().getContentAsString());
        return node.path("data").path("id").asLong();
    }

    private Long categoryId(String tk, Long ledgerId, String type) throws Exception {
        JsonNode cats = objectMapper.readTree(mockMvc.perform(
                get("/api/ledgers/" + ledgerId + "/categories").header("Authorization", "Bearer " + tk))
            .andReturn().getResponse().getContentAsString()).path("data");
        for (JsonNode c : cats) {
            if (type.equals(c.path("type").asText())) {
                return c.path("id").asLong();
            }
        }
        throw new IllegalStateException("no category of type " + type);
    }

    private Long createAccount(String tk, Long ledgerId) throws Exception {
        JsonNode node = objectMapper.readTree(mockMvc.perform(
                postJson("/api/ledgers/" + ledgerId + "/accounts",
                    "{\"name\":\"卡\",\"type\":\"asset\",\"initialBalance\":10000}")
                    .header("Authorization", "Bearer " + tk))
            .andReturn().getResponse().getContentAsString());
        return node.path("data").path("id").asLong();
    }

    private void createExpense(String tk, Long ledgerId, Long cat, Long acc, String amount, String date) throws Exception {
        mockMvc.perform(postJson("/api/ledgers/" + ledgerId + "/bills",
                "{\"type\":\"expense\",\"categoryId\":" + cat + ",\"amount\":" + amount + ",\"billDate\":\"" + date + "\"," +
                "\"items\":[{\"accountId\":" + acc + ",\"direction\":\"out\",\"amount\":" + amount + "}]}")
                .header("Authorization", "Bearer " + tk))
            .andExpect(status().isOk());
    }

    @Test
    void monthlyBudget_tracksUsageAndOverBudget() throws Exception {
        String tk = token("13800000070");
        Long ledgerId = createLedger(tk, "私账");
        Long cat = categoryId(tk, ledgerId, "expense");
        Long acc = createAccount(tk, ledgerId);
        String now = LocalDateTime.now().toString().replace("T", "T").substring(0, 19);

        // 设月度预算 200
        mockMvc.perform(postJson("/api/ledgers/" + ledgerId + "/budgets",
                "{\"categoryId\":" + cat + ",\"periodType\":\"monthly\",\"amount\":200}")
                .header("Authorization", "Bearer " + tk))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.periodType").value("monthly"))
            .andExpect(jsonPath("$.data.usage").value(0))
            .andExpect(jsonPath("$.data.overBudget").value(false));

        // 花 150 → 75%
        createExpense(tk, ledgerId, cat, acc, "150", now);
        mockMvc.perform(get("/api/ledgers/" + ledgerId + "/budgets")
                .header("Authorization", "Bearer " + tk))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].usage").value(150))
            .andExpect(jsonPath("$.data[0].percent").value(75.0))
            .andExpect(jsonPath("$.data[0].overBudget").value(false));

        // 再花 100 → 超支
        createExpense(tk, ledgerId, cat, acc, "100", now);
        mockMvc.perform(get("/api/ledgers/" + ledgerId + "/budgets")
                .header("Authorization", "Bearer " + tk))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].usage").value(250))
            .andExpect(jsonPath("$.data[0].overBudget").value(true));
    }

    @Test
    void customPeriodBudget_onlyCountsInRange() throws Exception {
        String tk = token("13800000071");
        Long ledgerId = createLedger(tk, "私账");
        Long cat = categoryId(tk, ledgerId, "expense");
        Long acc = createAccount(tk, ledgerId);
        LocalDate start = LocalDate.now().minusDays(10);
        LocalDate end = LocalDate.now().plusDays(10);
        // 范围内一笔 100，范围外一笔 50（30 天前）
        createExpense(tk, ledgerId, cat, acc, "100", LocalDateTime.now().toString().substring(0, 19));
        createExpense(tk, ledgerId, cat, acc, "50", LocalDateTime.now().minusDays(30).toString().substring(0, 19));

        mockMvc.perform(postJson("/api/ledgers/" + ledgerId + "/budgets",
                "{\"categoryId\":" + cat + ",\"periodType\":\"custom\",\"startDate\":\"" + start + "\",\"endDate\":\"" + end + "\",\"amount\":200}")
                .header("Authorization", "Bearer " + tk))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.usage").value(100));
    }

    @Test
    void duplicateCategoryBudget_rejected() throws Exception {
        String tk = token("13800000072");
        Long ledgerId = createLedger(tk, "私账");
        Long cat = categoryId(tk, ledgerId, "expense");
        mockMvc.perform(postJson("/api/ledgers/" + ledgerId + "/budgets",
                "{\"categoryId\":" + cat + ",\"periodType\":\"monthly\",\"amount\":100}")
                .header("Authorization", "Bearer " + tk)).andExpect(status().isOk());
        mockMvc.perform(postJson("/api/ledgers/" + ledgerId + "/budgets",
                "{\"categoryId\":" + cat + ",\"periodType\":\"monthly\",\"amount\":200}")
                .header("Authorization", "Bearer " + tk))
            .andExpect(status().isBadRequest());
    }

    @Test
    void updateAndDeleteBudget() throws Exception {
        String tk = token("13800000073");
        Long ledgerId = createLedger(tk, "私账");
        Long cat = categoryId(tk, ledgerId, "expense");
        JsonNode created = objectMapper.readTree(mockMvc.perform(
                postJson("/api/ledgers/" + ledgerId + "/budgets",
                    "{\"categoryId\":" + cat + ",\"periodType\":\"monthly\",\"amount\":100}")
                    .header("Authorization", "Bearer " + tk))
            .andReturn().getResponse().getContentAsString());
        Long id = created.path("data").path("id").asLong();

        mockMvc.perform(put("/api/budgets/" + id)
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("{\"categoryId\":" + cat + ",\"periodType\":\"monthly\",\"amount\":300}")
                .header("Authorization", "Bearer " + tk))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.amount").value(300));

        mockMvc.perform(delete("/api/budgets/" + id).header("Authorization", "Bearer " + tk))
            .andExpect(status().isOk());
        mockMvc.perform(get("/api/ledgers/" + ledgerId + "/budgets")
                .header("Authorization", "Bearer " + tk))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void otherUser_budget_forbidden() throws Exception {
        String tk = token("13800000074");
        String other = token("13800000075");
        Long ledgerId = createLedger(tk, "私账");
        Long cat = categoryId(tk, ledgerId, "expense");
        mockMvc.perform(postJson("/api/ledgers/" + ledgerId + "/budgets",
                "{\"categoryId\":" + cat + ",\"periodType\":\"monthly\",\"amount\":100}")
                .header("Authorization", "Bearer " + other))
            .andExpect(status().isForbidden());
    }
}
