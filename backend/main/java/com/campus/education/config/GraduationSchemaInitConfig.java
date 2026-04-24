package com.campus.education.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class GraduationSchemaInitConfig implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(GraduationSchemaInitConfig.class);

    private final JdbcTemplate jdbcTemplate;

    public GraduationSchemaInitConfig(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        jdbcTemplate.execute(
                "CREATE TABLE IF NOT EXISTS graduation_audit ("
                        + "audit_id VARCHAR(20) NOT NULL COMMENT '毕业审核ID',"
                        + "student_id VARCHAR(20) NOT NULL COMMENT '学号',"
                        + "total_credits DECIMAL(6,2) NOT NULL DEFAULT 0 COMMENT '已获总学分',"
                        + "required_credits DECIMAL(6,2) NOT NULL DEFAULT 0 COMMENT '要求总学分',"
                        + "compulsory_pass TINYINT(1) NOT NULL DEFAULT 0 COMMENT '必修课是否全部通过',"
                        + "elective_public_credits DECIMAL(6,2) NOT NULL DEFAULT 0 COMMENT '公共选修课学分',"
                        + "elective_major_credits DECIMAL(6,2) NOT NULL DEFAULT 0 COMMENT '专业选修课学分',"
                        + "gpa DECIMAL(4,2) NOT NULL DEFAULT 0 COMMENT '绩点',"
                        + "status ENUM('approved','rejected') NOT NULL COMMENT '审核状态',"
                        + "audit_opinion VARCHAR(255) NULL COMMENT '审核意见',"
                        + "degree_granted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已授予学位',"
                        + "degree_award_time DATETIME NULL COMMENT '学位授予时间',"
                        + "certificate_no VARCHAR(50) NULL COMMENT '证书编号',"
                        + "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',"
                        + "updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',"
                        + "PRIMARY KEY (audit_id),"
                        + "UNIQUE KEY uk_graduation_student (student_id),"
                        + "INDEX idx_graduation_status (status),"
                        + "CONSTRAINT fk_graduation_student FOREIGN KEY (student_id) REFERENCES student(student_id)"
                        + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='毕业审核表'"
        );
        log.info("Ensured graduation_audit table exists");
    }
}
