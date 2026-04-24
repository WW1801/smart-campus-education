package com.campus.education.config;

import com.campus.education.common.BusinessIdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class BusinessIdMigrationConfig implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(BusinessIdMigrationConfig.class);

    private final JdbcTemplate jdbcTemplate;
    private final BusinessIdGenerator businessIdGenerator;

    public BusinessIdMigrationConfig(JdbcTemplate jdbcTemplate, BusinessIdGenerator businessIdGenerator) {
        this.jdbcTemplate = jdbcTemplate;
        this.businessIdGenerator = businessIdGenerator;
    }

    @Override
    @Transactional
    public void run(String... args) {
        migrateRoles();
        migratePermissions();
        migrateUsers();
        migrateTeachingPlans();
    }

    private void migrateRoles() {
        List<String> invalidIds = jdbcTemplate.queryForList(
                "SELECT role_id FROM role WHERE role_id NOT REGEXP '^[0-9]{1,6}$' ORDER BY created_at, role_id",
                String.class
        );
        if (invalidIds.isEmpty()) {
            return;
        }

        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS=0");
        try {
            for (String oldId : invalidIds) {
                String newId = businessIdGenerator.nextNumericId("role", "role_id");
                jdbcTemplate.update("UPDATE role SET role_id = ? WHERE role_id = ?", newId, oldId);
                jdbcTemplate.update("UPDATE user SET role_id = ? WHERE role_id = ?", newId, oldId);
                jdbcTemplate.update("UPDATE role_permission SET role_id = ? WHERE role_id = ?", newId, oldId);
                log.info("Migrated role id {} -> {}", oldId, newId);
            }
        } finally {
            jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS=1");
        }
    }

    private void migratePermissions() {
        List<String> invalidIds = jdbcTemplate.queryForList(
                "SELECT permission_id FROM permission WHERE permission_id NOT REGEXP '^[0-9]{1,6}$' ORDER BY created_at, permission_id",
                String.class
        );
        if (invalidIds.isEmpty()) {
            return;
        }

        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS=0");
        try {
            for (String oldId : invalidIds) {
                String newId = businessIdGenerator.nextNumericId("permission", "permission_id");
                jdbcTemplate.update("UPDATE permission SET permission_id = ? WHERE permission_id = ?", newId, oldId);
                jdbcTemplate.update("UPDATE role_permission SET permission_id = ? WHERE permission_id = ?", newId, oldId);
                log.info("Migrated permission id {} -> {}", oldId, newId);
            }
        } finally {
            jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS=1");
        }
    }

    private void migrateUsers() {
        List<String> invalidIds = jdbcTemplate.queryForList(
                "SELECT user_id FROM user WHERE user_id NOT REGEXP '^[0-9]{1,6}$' ORDER BY created_at, user_id",
                String.class
        );
        for (String oldId : invalidIds) {
            String newId = businessIdGenerator.nextNumericId("user", "user_id");
            jdbcTemplate.update("UPDATE user SET user_id = ? WHERE user_id = ?", newId, oldId);
            jdbcTemplate.update("UPDATE audit_log SET user_id = ? WHERE user_id = ?", newId, oldId);
            log.info("Migrated user id {} -> {}", oldId, newId);
        }
    }

    private void migrateTeachingPlans() {
        List<String> invalidIds = jdbcTemplate.queryForList(
                "SELECT plan_id FROM teaching_plan WHERE plan_id NOT REGEXP '^TP[0-9]{3,}$' ORDER BY created_at, plan_id",
                String.class
        );
        for (String oldId : invalidIds) {
            String newId = businessIdGenerator.nextPrefixedId("teaching_plan", "plan_id", "TP", 3);
            jdbcTemplate.update("UPDATE teaching_plan SET plan_id = ? WHERE plan_id = ?", newId, oldId);
            log.info("Migrated teaching plan id {} -> {}", oldId, newId);
        }
    }
}
