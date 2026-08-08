-- ============================================================
-- 校园教务系统 完整建表脚本 v2
-- 兼容：MySQL 8.x / 10万学生 / 5万课程 / 并发1000
-- ============================================================

CREATE DATABASE IF NOT EXISTS education_system DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE education_system;

-- 创建院系表
CREATE TABLE IF NOT EXISTS department (
    department_id VARCHAR(20) PRIMARY KEY COMMENT '院系ID',
    name VARCHAR(100) NOT NULL UNIQUE COMMENT '院系名称',
    description TEXT COMMENT '院系描述',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='院系表';

-- 创建专业表
CREATE TABLE IF NOT EXISTS major (
    major_id VARCHAR(20) PRIMARY KEY COMMENT '专业ID',
    name VARCHAR(100) NOT NULL UNIQUE COMMENT '专业名称',
    department_id VARCHAR(20) NOT NULL COMMENT '院系ID',
    description TEXT COMMENT '专业描述',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_department (department_id),
    FOREIGN KEY (department_id) REFERENCES department(department_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='专业表';

-- 创建班级表
CREATE TABLE IF NOT EXISTS class (
    class_id VARCHAR(20) PRIMARY KEY COMMENT '班级ID',
    name VARCHAR(100) NOT NULL UNIQUE COMMENT '班级名称',
    major_id VARCHAR(20) NOT NULL COMMENT '专业ID',
    grade VARCHAR(10) NOT NULL COMMENT '年级',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_major (major_id),
    FOREIGN KEY (major_id) REFERENCES major(major_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='班级表';

-- 创建学生表
CREATE TABLE IF NOT EXISTS student (
    student_id VARCHAR(20) PRIMARY KEY COMMENT '内部学生ID',
    student_no VARCHAR(20) NOT NULL COMMENT '业务学号',
    name VARCHAR(50) NOT NULL COMMENT '姓名',
    gender ENUM('male', 'female') NOT NULL COMMENT '性别',
    birthdate DATE NOT NULL COMMENT '出生日期',
    phone VARCHAR(20) COMMENT '电话',
    email VARCHAR(100) COMMENT '邮箱',
    department_id VARCHAR(20) NOT NULL COMMENT '院系ID',
    major_id VARCHAR(20) NOT NULL COMMENT '专业ID',
    class_id VARCHAR(20) NOT NULL COMMENT '班级ID',
    enrollment_date DATE NOT NULL COMMENT '入学日期',
    status ENUM('active', 'suspended', 'graduated', 'dropped') NOT NULL COMMENT '学籍状态：active-在读 suspended-休学 graduated-毕业 dropped-退学',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_student_no (student_no),
    INDEX idx_department (department_id),
    INDEX idx_major (major_id),
    INDEX idx_class (class_id),
    INDEX idx_status (status),
    FOREIGN KEY (department_id) REFERENCES department(department_id),
    FOREIGN KEY (major_id) REFERENCES major(major_id),
    FOREIGN KEY (class_id) REFERENCES class(class_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生表';

-- 创建教师表
CREATE TABLE IF NOT EXISTS teacher (
    teacher_id VARCHAR(20) PRIMARY KEY COMMENT '工号',
    name VARCHAR(50) NOT NULL COMMENT '姓名',
    gender ENUM('male', 'female') NOT NULL COMMENT '性别',
    birthdate DATE NOT NULL COMMENT '出生日期',
    phone VARCHAR(20) COMMENT '电话',
    email VARCHAR(100) COMMENT '邮箱',
    department_id VARCHAR(20) NOT NULL COMMENT '院系ID',
    title VARCHAR(50) COMMENT '职称',
    specialty VARCHAR(100) COMMENT '专业方向',
    status ENUM('active', 'leave', 'resigned') NOT NULL COMMENT '状态：active-在职 leave-休假 resigned-离职',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_department (department_id),
    INDEX idx_status (status),
    FOREIGN KEY (department_id) REFERENCES department(department_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='教师表';

-- 创建课程表
CREATE TABLE IF NOT EXISTS course (
    course_id VARCHAR(20) PRIMARY KEY COMMENT '课程ID',
    name VARCHAR(100) NOT NULL COMMENT '课程名称',
    code VARCHAR(20) NOT NULL UNIQUE COMMENT '课程代码',
    credits DECIMAL(3,1) NOT NULL COMMENT '学分',
    hours INTEGER NOT NULL COMMENT '课时',
    type ENUM('compulsory', 'elective') NOT NULL COMMENT '课程类型：compulsory-必修 elective-选修',
    department_id VARCHAR(20) NOT NULL COMMENT '院系ID',
    description TEXT COMMENT '课程描述',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_department (department_id),
    INDEX idx_type (type),
    FOREIGN KEY (department_id) REFERENCES department(department_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程表';

-- 创建学期表
CREATE TABLE IF NOT EXISTS semester (
    semester_id VARCHAR(20) PRIMARY KEY COMMENT '学期ID',
    name VARCHAR(50) NOT NULL UNIQUE COMMENT '学期名称',
    start_date DATE NOT NULL COMMENT '开始日期',
    end_date DATE NOT NULL COMMENT '结束日期',
    teaching_weeks INTEGER NOT NULL COMMENT '教学周数',
    status ENUM('upcoming', 'current', 'completed') NOT NULL COMMENT '状态：upcoming-未开始 current-进行中 completed-已结束',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学期表';

-- 创建教室表
CREATE TABLE IF NOT EXISTS classroom (
    classroom_id VARCHAR(20) PRIMARY KEY COMMENT '教室ID',
    name VARCHAR(50) NOT NULL UNIQUE COMMENT '教室名称',
    capacity INTEGER NOT NULL COMMENT '容量',
    building VARCHAR(50) NOT NULL COMMENT '楼栋',
    type ENUM('classroom', 'laboratory', 'lecture_hall') NOT NULL COMMENT '类型：classroom-普通教室 laboratory-实验室 lecture_hall-阶梯教室',
    status ENUM('available', 'maintenance', 'occupied') NOT NULL COMMENT '状态：available-可用 maintenance-维护 occupied-占用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='教室表';

-- 创建课程安排表（支持按班级排课和开放选课两种模式）
CREATE TABLE IF NOT EXISTS course_schedule (
    schedule_id VARCHAR(20) PRIMARY KEY COMMENT '课程安排ID',
    mode ENUM('class_based', 'open_selection') NOT NULL DEFAULT 'class_based' COMMENT '排课模式：class_based-按班级排课 open_selection-开放选课',
    max_students INT NULL COMMENT '选课容量上限（仅开放选课模式有效）',
    current_students INT NOT NULL DEFAULT 0 COMMENT '当前已选人数（仅开放选课模式有效）',
    course_id VARCHAR(20) NOT NULL COMMENT '课程ID',
    teacher_id VARCHAR(20) NOT NULL COMMENT '教师ID',
    class_id VARCHAR(20) NULL COMMENT '班级ID（按班级排课时必填，开放选课时为空）',
    semester_id VARCHAR(20) NOT NULL COMMENT '学期ID',
    classroom_id VARCHAR(20) NOT NULL COMMENT '教室ID',
    day_of_week INTEGER NOT NULL COMMENT '星期几（1=周一 7=周日）',
    start_period INTEGER NOT NULL COMMENT '开始节次',
    end_period INTEGER NOT NULL COMMENT '结束节次',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_course (course_id),
    INDEX idx_teacher (teacher_id),
    INDEX idx_class (class_id),
    INDEX idx_semester (semester_id),
    INDEX idx_classroom (classroom_id),
    INDEX idx_teacher_time (semester_id, teacher_id, day_of_week, start_period, end_period),
    INDEX idx_classroom_time (semester_id, classroom_id, day_of_week, start_period, end_period),
    INDEX idx_class_time (semester_id, class_id, day_of_week, start_period, end_period),
    CONSTRAINT fk_schedule_course FOREIGN KEY (course_id) REFERENCES course(course_id),
    CONSTRAINT fk_schedule_teacher FOREIGN KEY (teacher_id) REFERENCES teacher(teacher_id),
    CONSTRAINT fk_schedule_class FOREIGN KEY (class_id) REFERENCES class(class_id),
    CONSTRAINT fk_schedule_semester FOREIGN KEY (semester_id) REFERENCES semester(semester_id),
    CONSTRAINT fk_schedule_classroom FOREIGN KEY (classroom_id) REFERENCES classroom(classroom_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程安排表-支持按班级排课和开放选课两种模式';

-- 创建成绩表（支持审核流程）
CREATE TABLE IF NOT EXISTS grade (
    grade_id VARCHAR(20) PRIMARY KEY COMMENT '成绩ID',
    student_id VARCHAR(20) NOT NULL COMMENT '学号',
    course_id VARCHAR(20) NOT NULL COMMENT '课程ID',
    semester_id VARCHAR(20) NOT NULL COMMENT '学期ID',
    teacher_id VARCHAR(20) NOT NULL COMMENT '教师ID',
    usual_score DECIMAL(5,2) NULL COMMENT '平时成绩（0~100）',
    exam_score DECIMAL(5,2) NULL COMMENT '考试成绩（0~100）',
    total_score DECIMAL(5,2) NOT NULL COMMENT '总成绩（0~100）',
    status ENUM('draft', 'submitted', 'approved', 'rejected') NOT NULL COMMENT '成绩状态：draft-待录入 submitted-待审核 approved-审核通过 rejected-审核驳回',
    is_pass TINYINT(1) NULL COMMENT '是否及格（0-不及格 1-及格，审核通过后自动判定）',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_student_semester (student_id, semester_id),
    INDEX idx_course_semester (course_id, semester_id),
    INDEX idx_teacher_semester (teacher_id, semester_id),
    INDEX idx_status (status),
    FOREIGN KEY (student_id) REFERENCES student(student_id),
    FOREIGN KEY (course_id) REFERENCES course(course_id),
    FOREIGN KEY (semester_id) REFERENCES semester(semester_id),
    FOREIGN KEY (teacher_id) REFERENCES teacher(teacher_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='成绩表-支持审核流程（draft/submitted/approved/rejected）';

-- 创建考勤表
CREATE TABLE IF NOT EXISTS attendance (
    attendance_id VARCHAR(20) PRIMARY KEY COMMENT '考勤ID',
    student_id VARCHAR(20) NOT NULL COMMENT '学号',
    course_id VARCHAR(20) NOT NULL COMMENT '课程ID',
    semester_id VARCHAR(20) NOT NULL COMMENT '学期ID',
    date DATE NOT NULL COMMENT '考勤日期',
    status ENUM('present', 'absent', 'late', 'leave', 'early') NOT NULL COMMENT '考勤状态：present-出勤 absent-旷课 late-迟到 leave-请假 early-早退',
    record_source ENUM('manual', 'leave_request') NOT NULL DEFAULT 'manual' COMMENT '记录来源：manual-人工 leave_request-请假审批',
    source_request_id VARCHAR(20) NULL COMMENT '关联请假申请ID',
    approved_by VARCHAR(20) NULL COMMENT '请假审批人用户ID',
    synced_at DATETIME NULL COMMENT '请假审批同步时间',
    manual_locked TINYINT(1) NOT NULL DEFAULT 0 COMMENT '人工锁定：1-禁止自动或普通人工覆盖',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_student_course (student_id, course_id),
    INDEX idx_date (date),
    INDEX idx_attendance_source_request (source_request_id),
    UNIQUE KEY uk_attendance_student_course_semester_date (student_id, course_id, semester_id, date),
    FOREIGN KEY (student_id) REFERENCES student(student_id),
    FOREIGN KEY (course_id) REFERENCES course(course_id),
    FOREIGN KEY (semester_id) REFERENCES semester(semester_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考勤表';

-- 创建角色表
CREATE TABLE IF NOT EXISTS role (
    role_id VARCHAR(20) PRIMARY KEY COMMENT '角色ID',
    name VARCHAR(50) NOT NULL UNIQUE COMMENT '角色名称',
    description TEXT COMMENT '角色描述',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- 创建权限表
CREATE TABLE IF NOT EXISTS permission (
    permission_id VARCHAR(20) PRIMARY KEY COMMENT '权限ID',
    name VARCHAR(50) NOT NULL UNIQUE COMMENT '权限名称',
    code VARCHAR(50) NOT NULL UNIQUE COMMENT '权限代码',
    description TEXT COMMENT '权限描述',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限表';

-- 创建角色权限关联表（多对多）
CREATE TABLE IF NOT EXISTS role_permission (
    role_id VARCHAR(20) NOT NULL COMMENT '角色ID',
    permission_id VARCHAR(20) NOT NULL COMMENT '权限ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (role_id, permission_id),
    INDEX idx_role (role_id),
    INDEX idx_permission (permission_id),
    FOREIGN KEY (role_id) REFERENCES role(role_id),
    FOREIGN KEY (permission_id) REFERENCES permission(permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色权限关联表-实现角色与权限多对多关系';

-- 创建用户表
CREATE TABLE IF NOT EXISTS user (
    user_id VARCHAR(20) PRIMARY KEY COMMENT '用户ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(100) NOT NULL COMMENT '密码（BCrypt加密存储）',
    name VARCHAR(50) NOT NULL COMMENT '姓名',
    role_id VARCHAR(20) NOT NULL COMMENT '角色ID',
    related_id VARCHAR(20) NULL COMMENT '关联ID（学生学号或教师工号）',
    last_login DATETIME NULL COMMENT '最后登录时间',
    login_ip VARCHAR(50) NULL COMMENT '登录IP',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_role (role_id),
    INDEX idx_related (related_id),
    FOREIGN KEY (role_id) REFERENCES role(role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 创建请假申请表；活动申请指 pending/approved，用生成列实现并发重复提交保护。
CREATE TABLE IF NOT EXISTS leave_request (
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

-- ============================================================
-- 新增数据表
-- ============================================================

-- 创建选课记录表
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

-- 创建教学计划表
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

-- 创建审计日志表
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

CREATE TABLE IF NOT EXISTS academic_warning_record (
    record_id       VARCHAR(20)   NOT NULL COMMENT '预警记录ID',
    student_id      VARCHAR(20)   NOT NULL COMMENT '学生ID',
    semester_id     VARCHAR(20)   NULL COMMENT '学期ID',
    risk_level      VARCHAR(20)   NOT NULL COMMENT '风险等级 high/medium/low',
    risk_score      INT           NULL COMMENT '风险分数',
    risk_reason     VARCHAR(1000) NOT NULL COMMENT '风险原因',
    triggered_rules TEXT          NULL COMMENT '命中规则JSON',
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

-- ============================================================
-- 初始数据
-- ============================================================
