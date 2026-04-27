package com.campus.education.common;

/**
 * 业务编号生成器，负责生成系统内的业务主键编号。
 */

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class BusinessIdGenerator {

    private final JdbcTemplate jdbcTemplate;

    // 处理业务编号生成器
    public BusinessIdGenerator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // 处理下一个数字编号
    public String nextNumericId(String table, String column) {
        String sql = String.format(
                "SELECT COALESCE(MAX(CAST(%s AS UNSIGNED)), 0) FROM %s WHERE %s REGEXP '^[0-9]{1,6}$'",
                column, table, column
        );
        Integer max = jdbcTemplate.queryForObject(sql, Integer.class);
        return String.valueOf((max == null ? 0 : max) + 1);
    }

    // 处理下一个前缀编号
    public String nextPrefixedId(String table, String column, String prefix, int padLength) {
        String regex = String.format("^%s[0-9]{%d,}$", prefix, padLength);
        String sql = String.format(
                "SELECT COALESCE(MAX(CAST(SUBSTRING(%s, %d) AS UNSIGNED)), 0) FROM %s WHERE %s REGEXP '%s'",
                column, prefix.length() + 1, table, column, regex
        );
        Integer max = jdbcTemplate.queryForObject(sql, Integer.class);
        int next = (max == null ? 0 : max) + 1;
        return prefix + String.format("%0" + padLength + "d", next);
    }
}
