package com.familyhome;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Schema 索引：t_bill_account.account_id 应有索引，支撑按账户筛选账单/流水。
 */
@SpringBootTest
class SchemaIndexTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private int indexCount(String table, String indexName) {
        // MySQL 信息模式：INFORMATION_SCHEMA.STATISTICS；表名/索引名大小写不敏感
        Integer c = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS " +
            "WHERE TABLE_SCHEMA = DATABASE() AND LOWER(TABLE_NAME) = LOWER(?) AND LOWER(INDEX_NAME) = LOWER(?)",
            Integer.class, table, indexName);
        return c == null ? 0 : c;
    }

    @Test
    void billAccount_hasAccountIdIndex() {
        assertEquals(1, indexCount("t_bill_account", "idx_account"),
            "t_bill_account 应存在 account_id 索引 idx_account");
    }

    @Test
    void billAccount_hasBillIdIndex() {
        assertEquals(1, indexCount("t_bill_account", "idx_bill"),
            "t_bill_account 应存在 bill_id 索引 idx_bill");
    }
}
