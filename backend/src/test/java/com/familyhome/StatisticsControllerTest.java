package com.familyhome;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Phase 7：统计验收（收支趋势、分类占比）。
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Rollback
class StatisticsControllerTest extends ApiTestBase {

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
        throw new IllegalStateException("no category type " + type);
    }

    private Long createAccount(String tk, Long ledgerId) throws Exception {
        JsonNode node = objectMapper.readTree(mockMvc.perform(
                postJson("/api/ledgers/" + ledgerId + "/accounts",
                    "{\"name\":\"卡\",\"type\":\"asset\",\"balance\":10000}")
                    .header("Authorization", "Bearer " + tk))
            .andReturn().getResponse().getContentAsString());
        return node.path("data").path("id").asLong();
    }

    private void createBill(String tk, Long ledgerId, String type, Long cat, Long acc, String amount, String date) throws Exception {
        String dir = "expense".equals(type) ? "out" : "in";
        mockMvc.perform(postJson("/api/ledgers/" + ledgerId + "/bills",
                "{\"type\":\"" + type + "\",\"categoryId\":" + cat + ",\"amount\":" + amount + ",\"billDate\":\"" + date + "\"," +
                "\"items\":[{\"accountId\":" + acc + ",\"direction\":\"" + dir + "\",\"amount\":" + amount + "}]}")
                .header("Authorization", "Bearer " + tk))
            .andExpect(status().isOk());
    }

    @Test
    void trend_aggregatesByMonth() throws Exception {
        String tk = token("13800000080");
        Long ledgerId = createLedger(tk, "私账");
        Long exp = categoryId(tk, ledgerId, "expense");
        Long inc = categoryId(tk, ledgerId, "income");
        Long acc = createAccount(tk, ledgerId);

        String month = LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"));
        LocalDate start = LocalDate.now().withDayOfMonth(1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());
        createBill(tk, ledgerId, "expense", exp, acc, "100", month + "-05T12:00:00");
        createBill(tk, ledgerId, "expense", exp, acc, "50", month + "-15T12:00:00");
        createBill(tk, ledgerId, "income", inc, acc, "500", month + "-10T12:00:00");

        mockMvc.perform(get("/api/ledgers/" + ledgerId + "/statistics/trend?startDate=" + start + "&endDate=" + end)
                .header("Authorization", "Bearer " + tk))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].period").value(month))
            .andExpect(jsonPath("$.data[0].expense").value(150))
            .andExpect(jsonPath("$.data[0].income").value(500));
    }

    @Test
    void trend_byDay() throws Exception {
        String tk = token("13800000081");
        Long ledgerId = createLedger(tk, "私账");
        Long exp = categoryId(tk, ledgerId, "expense");
        Long acc = createAccount(tk, ledgerId);
        String day = LocalDate.now().toString();
        createBill(tk, ledgerId, "expense", exp, acc, "88", day + "T08:00:00");
        mockMvc.perform(get("/api/ledgers/" + ledgerId + "/statistics/trend?groupBy=day&startDate=" + day + "&endDate=" + day)
                .header("Authorization", "Bearer " + tk))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].period").value(day))
            .andExpect(jsonPath("$.data[0].expense").value(88));
    }

    @Test
    void trend_byWeek_aggregatesWithinWeek() throws Exception {
        String tk = token("13800000084");
        Long ledgerId = createLedger(tk, "私账");
        Long exp = categoryId(tk, ledgerId, "expense");
        Long acc = createAccount(tk, ledgerId);
        // 本周周一与周二各记一笔（同一周）
        java.time.LocalDate monday = LocalDate.now()
            .with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
        java.time.LocalDate tuesday = monday.plusDays(1);
        String mondayKey = monday.toString();
        createBill(tk, ledgerId, "expense", exp, acc, "30", monday + "T09:00:00");
        createBill(tk, ledgerId, "expense", exp, acc, "20", tuesday + "T09:00:00");

        mockMvc.perform(get("/api/ledgers/" + ledgerId + "/statistics/trend?groupBy=week&startDate=" + monday + "&endDate=" + tuesday)
                .header("Authorization", "Bearer " + tk))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].period").value(mondayKey))
            .andExpect(jsonPath("$.data[0].expense").value(50));
    }

    @Test
    void trend_byDay_zeroFilledForMissingDays() throws Exception {
        String tk = token("13800000085");
        Long ledgerId = createLedger(tk, "私账");
        Long exp = categoryId(tk, ledgerId, "expense");
        Long acc = createAccount(tk, ledgerId);
        // 范围：昨天、今天、明天；仅在今天记一笔
        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(1);
        LocalDate end = today.plusDays(1);
        createBill(tk, ledgerId, "expense", exp, acc, "60", today + "T08:00:00");

        mockMvc.perform(get("/api/ledgers/" + ledgerId + "/statistics/trend?groupBy=day&startDate=" + start + "&endDate=" + end)
                .header("Authorization", "Bearer " + tk))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(3))
            .andExpect(jsonPath("$.data[1].period").value(today.toString()))
            .andExpect(jsonPath("$.data[1].expense").value(60))
            .andExpect(jsonPath("$.data[0].expense").value(0))
            .andExpect(jsonPath("$.data[2].expense").value(0))
            .andExpect(jsonPath("$.data[0].income").value(0));
    }

    @Test
    void trend_byMonth_zeroFilledAcrossMonths() throws Exception {
        String tk = token("13800000086");
        Long ledgerId = createLedger(tk, "私账");
        Long exp = categoryId(tk, ledgerId, "expense");
        Long acc = createAccount(tk, ledgerId);
        // 上月与本月，仅本月记一笔
        LocalDate firstOfLastMonth = LocalDate.now().minusMonths(1).withDayOfMonth(1);
        LocalDate lastOfThisMonth = LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth());
        String thisMonth = LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"));
        createBill(tk, ledgerId, "expense", exp, acc, "100", thisMonth + "-10T10:00:00");

        mockMvc.perform(get("/api/ledgers/" + ledgerId + "/statistics/trend?startDate=" + firstOfLastMonth + "&endDate=" + lastOfThisMonth)
                .header("Authorization", "Bearer " + tk))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(2))
            .andExpect(jsonPath("$.data[1].period").value(thisMonth))
            .andExpect(jsonPath("$.data[1].expense").value(100))
            .andExpect(jsonPath("$.data[0].expense").value(0));
    }

    @Test
    void category_expenseBreakdown_withPercent() throws Exception {
        String tk = token("13800000082");
        Long ledgerId = createLedger(tk, "私账");
        Long acc = createAccount(tk, ledgerId);
        String day = LocalDate.now().toString();

        JsonNode cats = objectMapper.readTree(mockMvc.perform(
                get("/api/ledgers/" + ledgerId + "/categories").header("Authorization", "Bearer " + tk))
            .andReturn().getResponse().getContentAsString()).path("data");
        java.util.List<Long> expenseIds = new java.util.ArrayList<>();
        for (JsonNode c : cats) {
            if ("expense".equals(c.path("type").asText())) {
                expenseIds.add(c.path("id").asLong());
            }
        }
        org.junit.jupiter.api.Assertions.assertTrue(expenseIds.size() >= 2, "默认应至少有 2 个支出分类");
        createBill(tk, ledgerId, "expense", expenseIds.get(0), acc, "30", day + "T08:00:00");
        createBill(tk, ledgerId, "expense", expenseIds.get(1), acc, "70", day + "T09:00:00");

        mockMvc.perform(get("/api/ledgers/" + ledgerId + "/statistics/category?type=expense")
                .header("Authorization", "Bearer " + tk))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(2))
            .andExpect(jsonPath("$.data[0].amount").value(70))
            .andExpect(jsonPath("$.data[0].percent").value(70.0))
            .andExpect(jsonPath("$.data[1].amount").value(30))
            .andExpect(jsonPath("$.data[1].percent").value(30.0));
    }

    @Test
    void category_invalidType_rejected() throws Exception {
        String tk = token("13800000083");
        Long ledgerId = createLedger(tk, "私账");
        mockMvc.perform(get("/api/ledgers/" + ledgerId + "/statistics/category?type=all")
                .header("Authorization", "Bearer " + tk))
            .andExpect(status().isBadRequest());
    }
}
