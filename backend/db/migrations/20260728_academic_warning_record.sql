-- 学业预警历史表迁移：现有环境关闭 SQL 自动初始化时需手动执行本文件。
CREATE TABLE IF NOT EXISTS academic_warning_record (
    record_id       VARCHAR(20)   NOT NULL COMMENT '预警记录ID',
    student_id      VARCHAR(20)   NOT NULL COMMENT '学生内部ID，对应 student.student_id',
    semester_id     VARCHAR(20)   NULL COMMENT '学期ID',
    risk_level      VARCHAR(20)   NOT NULL COMMENT '风险等级 high/medium/low',
    risk_score      INT           NULL COMMENT '风险分数',
    risk_reason     VARCHAR(1000) NOT NULL COMMENT '风险原因',
    triggered_rules TEXT          NULL COMMENT '命中规则 JSON 数组',
    calculated_at   DATETIME      NOT NULL COMMENT '计算时间',
    process_status  VARCHAR(20)   NOT NULL DEFAULT 'pending' COMMENT '处理状态 pending/processing/completed',
    process_opinion VARCHAR(1000) NULL COMMENT '处理意见',
    processed_by    VARCHAR(20)   NULL COMMENT '处理人用户ID',
    processed_at    DATETIME      NULL COMMENT '处理时间',
    created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (record_id),
    INDEX idx_warning_student (student_id, calculated_at),
    INDEX idx_warning_status (process_status),
    INDEX idx_warning_level (risk_level),
    CONSTRAINT fk_warning_student FOREIGN KEY (student_id) REFERENCES student(student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学业预警历史和处理记录表';
