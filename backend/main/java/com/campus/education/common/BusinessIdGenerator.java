package com.campus.education.common;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class BusinessIdGenerator {

    private final JdbcTemplate jdbcTemplate;

    public BusinessIdGenerator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public String nextNumericId(String table, String column) {
        String sql = String.format(
                "SELECT COALESCE(MAX(CAST(%s AS UNSIGNED)), 0) FROM %s WHERE %s REGEXP '^[0-9]{1,6}$'",
                column, table, column
        );
        Integer max = jdbcTemplate.queryForObject(sql, Integer.class);
        return String.valueOf((max == null ? 0 : max) + 1);
    }

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
