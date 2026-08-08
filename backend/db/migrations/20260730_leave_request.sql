-- 前置条件：已执行正式 schema 至 user、attendance、student/course/semester 表均存在，
-- 且 attendance 已具备 (student_id, course_id, semester_id, date) 唯一键。
-- 本迁移只创建请假表并扩充考勤来源字段，不初始化或修改既有业务数据。

CREATE TABLE leave_request (
    request_id VARCHAR(20) PRIMARY KEY COMMENT '请假申请ID',
    student_id VARCHAR(20) NOT NULL COMMENT '学生内部ID',
    course_id VARCHAR(20) NULL COMMENT '课程ID，可为空表示不关联具体课程',
    semester_id VARCHAR(20) NOT NULL COMMENT '学期ID',
    start_date DATE NOT NULL COMMENT '开始日期',
    end_date DATE NOT NULL COMMENT '结束日期',
    leave_type ENUM('sick', 'personal', 'official', 'other') NOT NULL COMMENT '请假类型',
    reason VARCHAR(1000) NOT NULL COMMENT '请假原因',
    status ENUM('pending', 'approved', 'rejected', 'cancelled') NOT NULL DEFAULT 'pending' COMMENT '申请状态',
    submitted_at DATETIME NOT NULL COMMENT '提交时间',
    processed_by VARCHAR(20) NULL COMMENT '审批人用户ID',
    processed_at DATETIME NULL COMMENT '审批时间',
    process_opinion VARCHAR(1000) NULL COMMENT '审批意见',
    attendance_sync_status ENUM('pending', 'synced', 'failed', 'not_required') NOT NULL DEFAULT 'pending' COMMENT '考勤同步状态',
    sync_failure_reason VARCHAR(1000) NULL COMMENT '最近一次同步失败原因',
    request_hash CHAR(64) NOT NULL COMMENT '重复提交指纹',
    active_request_hash CHAR(64) GENERATED ALWAYS AS (
        CASE WHEN status IN ('pending', 'approved') THEN request_hash ELSE NULL END
    ) STORED COMMENT '活动申请唯一指纹',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_leave_active_hash (active_request_hash),
    INDEX idx_leave_student_submitted (student_id, submitted_at),
    INDEX idx_leave_admin_filter (semester_id, course_id, status, start_date, end_date),
    INDEX idx_leave_processed_by (processed_by),
    CONSTRAINT fk_leave_student FOREIGN KEY (student_id) REFERENCES student(student_id),
    CONSTRAINT fk_leave_course FOREIGN KEY (course_id) REFERENCES course(course_id),
    CONSTRAINT fk_leave_semester FOREIGN KEY (semester_id) REFERENCES semester(semester_id),
    CONSTRAINT fk_leave_processor FOREIGN KEY (processed_by) REFERENCES user(user_id),
    CONSTRAINT chk_leave_date_range CHECK (end_date >= start_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生请假申请与审批同步记录';

ALTER TABLE attendance
    ADD COLUMN record_source ENUM('manual', 'leave_request') NOT NULL DEFAULT 'manual'
        COMMENT '记录来源：manual-人工 leave_request-请假审批' AFTER status,
    ADD COLUMN source_request_id VARCHAR(20) NULL COMMENT '关联请假申请ID' AFTER record_source,
    ADD COLUMN approved_by VARCHAR(20) NULL COMMENT '请假审批人用户ID' AFTER source_request_id,
    ADD COLUMN synced_at DATETIME NULL COMMENT '请假审批同步时间' AFTER approved_by,
    ADD COLUMN manual_locked TINYINT(1) NOT NULL DEFAULT 0
        COMMENT '人工锁定：1-禁止自动或普通人工覆盖' AFTER synced_at,
    ADD INDEX idx_attendance_source_request (source_request_id);
