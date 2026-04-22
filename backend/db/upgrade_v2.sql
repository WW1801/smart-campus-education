-- ============================================================
-- 校园教务系统 数据库增量升级脚本 v2
-- 说明：在 create_tables.sql 基础上补充缺失的数据模型
-- 新增表：student_course_selection / teaching_plan / audit_log
-- 修改表：grade / course_schedule
-- 兼容：MySQL 8.x / 10万学生 / 5万课程 / 并发1000
-- ============================================================

USE education_system;

-- ============================================================
-- 第一部分：修改现有表结构
-- ============================================================

-- 1.1 修改成绩表：status 枚举值改为支持审核流程 + 新增 is_pass 字段
ALTER TABLE grade
    MODIFY COLUMN status ENUM('pending', 'approved', 'rejected') NOT NULL COMMENT '成绩状态：pending-待审核 approved-审核通过 rejected-审核驳回',
    ADD COLUMN is_pass TINYINT(1) NULL COMMENT '是否及格（0-不及格 1-及格，审核通过后自动判定）' AFTER status;

-- 1.2 修改课程安排表：支持选修课排课模式
ALTER TABLE course_schedule
    MODIFY COLUMN class_id VARCHAR(20) NULL COMMENT '班级ID（按班级排课时必填，开放选课时为空）',
    ADD COLUMN mode ENUM('class_based', 'open_selection') NOT NULL DEFAULT 'class_based' COMMENT '排课模式：class_based-按班级排课 open_selection-开放选课' AFTER schedule_id,
    ADD COLUMN max_students INT NULL COMMENT '选课容量上限（仅开放选课模式有效）' AFTER mode,
    ADD COLUMN current_students INT NOT NULL DEFAULT 0 COMMENT '当前已选人数（仅开放选课模式有效）' AFTER max_students;

-- 1.3 为课程安排表添加排课冲突检测索引
ALTER TABLE course_schedule
    ADD INDEX idx_teacher_time (semester_id, teacher_id, day_of_week, start_period, end_period),
    ADD INDEX idx_classroom_time (semester_id, classroom_id, day_of_week, start_period, end_period),
    ADD INDEX idx_class_time (semester_id, class_id, day_of_week, start_period, end_period);

-- 1.4 为已有高频查询表补充索引
ALTER TABLE student
    ADD INDEX idx_department (department_id),
    ADD INDEX idx_major (major_id),
    ADD INDEX idx_class (class_id),
    ADD INDEX idx_status (status);

ALTER TABLE teacher
    ADD INDEX idx_department (department_id),
    ADD INDEX idx_status (status);

ALTER TABLE grade
    ADD INDEX idx_student_semester (student_id, semester_id),
    ADD INDEX idx_course_semester (course_id, semester_id),
    ADD INDEX idx_teacher_semester (teacher_id, semester_id),
    ADD INDEX idx_status (status);

ALTER TABLE attendance
    ADD INDEX idx_student_course (student_id, course_id),
    ADD INDEX idx_date (date);

ALTER TABLE course
    ADD INDEX idx_department (department_id),
    ADD INDEX idx_type (type);

-- ============================================================
-- 第二部分：新增数据表
-- ============================================================

-- 2.1 选课记录表
CREATE TABLE IF NOT EXISTS student_course_selection (
    selection_id     VARCHAR(20)   NOT NULL COMMENT '选课记录ID',
    student_id       VARCHAR(20)   NOT NULL COMMENT '学号',
    course_id        VARCHAR(20)   NOT NULL COMMENT '课程ID',
    schedule_id      VARCHAR(20)   NOT NULL COMMENT '课程安排ID（关联具体排课）',
    semester_id      VARCHAR(20)   NOT NULL COMMENT '学期ID',
    status           ENUM('selected', 'dropped', 'completed') NOT NULL COMMENT '选课状态：selected-已选 dropped-已退选 completed-已完成',
    selection_time   DATETIME      NOT NULL COMMENT '选课操作时间',
    drop_time        DATETIME      NULL     COMMENT '退选操作时间',
    created_at       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (selection_id),
    UNIQUE KEY uk_student_schedule (student_id, schedule_id),
    INDEX idx_student_semester (student_id, semester_id),
    INDEX idx_course_semester (course_id, semester_id),
    INDEX idx_schedule (schedule_id),
    INDEX idx_status (status),
    CONSTRAINT fk_selection_student FOREIGN KEY (student_id) REFERENCES student(student_id),
    CONSTRAINT fk_selection_course FOREIGN KEY (course_id) REFERENCES course(course_id),
    CONSTRAINT fk_selection_schedule FOREIGN KEY (schedule_id) REFERENCES course_schedule(schedule_id),
    CONSTRAINT fk_selection_semester FOREIGN KEY (semester_id) REFERENCES semester(semester_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='选课记录表-记录学生选课/退选信息';

-- 2.2 教学计划表
CREATE TABLE IF NOT EXISTS teaching_plan (
    plan_id          VARCHAR(20)   NOT NULL COMMENT '教学计划ID',
    major_id         VARCHAR(20)   NOT NULL COMMENT '专业ID',
    course_id        VARCHAR(20)   NOT NULL COMMENT '课程ID',
    semester_type    TINYINT       NOT NULL COMMENT '建议修读学期（1~8，对应大一至大四各学期）',
    course_nature    ENUM('compulsory', 'elective_public', 'elective_major') NOT NULL COMMENT '课程性质：compulsory-必修 elective_public-公共选修 elective_major-专业选修',
    is_prerequisite  TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '是否为先修课程（0-否 1-是）',
    prerequisite_ids VARCHAR(200)  NULL     COMMENT '先修课程ID列表（逗号分隔，如 CO001,CO002）',
    created_at       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (plan_id),
    UNIQUE KEY uk_major_course (major_id, course_id),
    INDEX idx_major_semester (major_id, semester_type),
    INDEX idx_course (course_id),
    INDEX idx_nature (course_nature),
    CONSTRAINT fk_plan_major FOREIGN KEY (major_id) REFERENCES major(major_id),
    CONSTRAINT fk_plan_course FOREIGN KEY (course_id) REFERENCES course(course_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='教学计划表-定义各专业培养方案中的课程设置';

-- 2.3 审计日志表
CREATE TABLE IF NOT EXISTS audit_log (
    log_id           BIGINT        NOT NULL AUTO_INCREMENT COMMENT '日志ID（自增）',
    user_id          VARCHAR(20)   NULL     COMMENT '操作用户ID',
    username         VARCHAR(50)   NULL     COMMENT '操作用户名（冗余，防用户删除后丢失）',
    operation        VARCHAR(30)   NOT NULL COMMENT '操作类型：CREATE/UPDATE/DELETE/LOGIN/LOGOUT',
    module           VARCHAR(50)   NOT NULL COMMENT '操作模块（如 student/course/grade）',
    target_type      VARCHAR(50)   NULL     COMMENT '操作对象类型（如 Student/Course）',
    target_id        VARCHAR(20)   NULL     COMMENT '操作对象ID',
    detail           JSON          NULL     COMMENT '操作详情（JSON格式，记录变更前后数据）',
    ip               VARCHAR(50)   NULL     COMMENT '操作IP地址',
    user_agent       VARCHAR(500)  NULL     COMMENT '客户端User-Agent',
    created_at       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    PRIMARY KEY (log_id),
    INDEX idx_user (user_id),
    INDEX idx_module (module),
    INDEX idx_operation (operation),
    INDEX idx_target (target_type, target_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审计日志表-记录系统关键操作日志';

-- ============================================================
-- 第三部分：补充角色权限关联表增强（原表已存在，补充索引和字段）
-- ============================================================

ALTER TABLE role_permission
    ADD COLUMN created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间' AFTER permission_id,
    ADD INDEX idx_role (role_id),
    ADD INDEX idx_permission (permission_id);

-- ============================================================
-- 第四部分：补充权限初始数据（选课相关）
-- ============================================================

INSERT INTO permission (permission_id, name, code, description) VALUES
('17', '选课管理', 'course:selection:manage', '管理学生选课'),
('18', '成绩查询', 'grade:query', '查询个人/课程成绩'),
('19', '课表查询', 'schedule:query', '查询课表信息'),
('20', '考勤查询', 'attendance:query', '查询个人考勤记录')
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- 学生角色补充权限：选课管理、成绩查询、课表查询、考勤查询
INSERT INTO role_permission (role_id, permission_id) VALUES
('5', '17'), ('5', '18'), ('5', '19'), ('5', '20')
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

-- 教师角色补充权限：课表查询
INSERT INTO role_permission (role_id, permission_id) VALUES
('4', '19')
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);
CREATE TABLE IF NOT EXISTS graduation_audit (
    audit_id               VARCHAR(20)   NOT NULL COMMENT '毕业审核ID',
    student_id             VARCHAR(20)   NOT NULL COMMENT '学号',
    total_credits          DECIMAL(6,2)  NOT NULL DEFAULT 0 COMMENT '已获总学分',
    required_credits       DECIMAL(6,2)  NOT NULL DEFAULT 0 COMMENT '要求总学分',
    compulsory_pass        TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '必修课是否全部通过',
    elective_public_credits DECIMAL(6,2) NOT NULL DEFAULT 0 COMMENT '公共选修课学分',
    elective_major_credits DECIMAL(6,2)  NOT NULL DEFAULT 0 COMMENT '专业选修课学分',
    gpa                    DECIMAL(4,2)  NOT NULL DEFAULT 0 COMMENT '绩点',
    status                 ENUM('approved', 'rejected') NOT NULL COMMENT '审核状态',
    audit_opinion          VARCHAR(255)  NULL COMMENT '审核意见',
    degree_granted         TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '是否已授予学位',
    degree_award_time      DATETIME      NULL COMMENT '学位授予时间',
    certificate_no         VARCHAR(50)   NULL COMMENT '证书编号',
    created_at             DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at             DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (audit_id),
    UNIQUE KEY uk_graduation_student (student_id),
    INDEX idx_graduation_status (status),
    CONSTRAINT fk_graduation_student FOREIGN KEY (student_id) REFERENCES student(student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='毕业审核表';
