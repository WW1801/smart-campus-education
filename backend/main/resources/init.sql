-- ============================================================
-- 校园教务系统 Spring Boot 初始化脚本 v2
-- 与 db/create_tables.sql 保持同步
-- ============================================================

CREATE DATABASE IF NOT EXISTS education_system DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE education_system;

CREATE TABLE IF NOT EXISTS department (
    department_id VARCHAR(20) PRIMARY KEY COMMENT '院系ID',
    name VARCHAR(100) NOT NULL UNIQUE COMMENT '院系名称',
    description TEXT COMMENT '院系描述',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='院系表';

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

CREATE TABLE IF NOT EXISTS student (
    student_id VARCHAR(20) PRIMARY KEY COMMENT '学号',
    name VARCHAR(50) NOT NULL COMMENT '姓名',
    gender ENUM('male', 'female') NOT NULL COMMENT '性别',
    birthdate DATE NOT NULL COMMENT '出生日期',
    phone VARCHAR(20) COMMENT '电话',
    email VARCHAR(100) COMMENT '邮箱',
    department_id VARCHAR(20) NOT NULL COMMENT '院系ID',
    major_id VARCHAR(20) NOT NULL COMMENT '专业ID',
    class_id VARCHAR(20) NOT NULL COMMENT '班级ID',
    enrollment_date DATE NOT NULL COMMENT '入学日期',
    status ENUM('active', 'suspended', 'graduated', 'dropped') NOT NULL COMMENT '学籍状态',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_department (department_id),
    INDEX idx_major (major_id),
    INDEX idx_class (class_id),
    INDEX idx_status (status),
    FOREIGN KEY (department_id) REFERENCES department(department_id),
    FOREIGN KEY (major_id) REFERENCES major(major_id),
    FOREIGN KEY (class_id) REFERENCES class(class_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生表';

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
    status ENUM('active', 'leave', 'resigned') NOT NULL COMMENT '状态',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_department (department_id),
    INDEX idx_status (status),
    FOREIGN KEY (department_id) REFERENCES department(department_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='教师表';

CREATE TABLE IF NOT EXISTS course (
    course_id VARCHAR(20) PRIMARY KEY COMMENT '课程ID',
    name VARCHAR(100) NOT NULL COMMENT '课程名称',
    code VARCHAR(20) NOT NULL UNIQUE COMMENT '课程代码',
    credits DECIMAL(3,1) NOT NULL COMMENT '学分',
    hours INTEGER NOT NULL COMMENT '课时',
    type ENUM('compulsory', 'elective') NOT NULL COMMENT '课程类型',
    department_id VARCHAR(20) NOT NULL COMMENT '院系ID',
    description TEXT COMMENT '课程描述',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_department (department_id),
    INDEX idx_type (type),
    FOREIGN KEY (department_id) REFERENCES department(department_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程表';

CREATE TABLE IF NOT EXISTS semester (
    semester_id VARCHAR(20) PRIMARY KEY COMMENT '学期ID',
    name VARCHAR(50) NOT NULL UNIQUE COMMENT '学期名称',
    start_date DATE NOT NULL COMMENT '开始日期',
    end_date DATE NOT NULL COMMENT '结束日期',
    teaching_weeks INTEGER NOT NULL COMMENT '教学周数',
    status ENUM('upcoming', 'current', 'completed') NOT NULL COMMENT '状态',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学期表';

CREATE TABLE IF NOT EXISTS classroom (
    classroom_id VARCHAR(20) PRIMARY KEY COMMENT '教室ID',
    name VARCHAR(50) NOT NULL UNIQUE COMMENT '教室名称',
    capacity INTEGER NOT NULL COMMENT '容量',
    building VARCHAR(50) NOT NULL COMMENT '楼栋',
    type ENUM('classroom', 'laboratory', 'lecture_hall') NOT NULL COMMENT '类型',
    status ENUM('available', 'maintenance', 'occupied') NOT NULL COMMENT '状态',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='教室表';

CREATE TABLE IF NOT EXISTS course_schedule (
    schedule_id VARCHAR(20) PRIMARY KEY COMMENT '课程安排ID',
    mode ENUM('class_based', 'open_selection') NOT NULL DEFAULT 'class_based' COMMENT '排课模式',
    max_students INT NULL COMMENT '选课容量上限',
    current_students INT NOT NULL DEFAULT 0 COMMENT '当前已选人数',
    course_id VARCHAR(20) NOT NULL COMMENT '课程ID',
    teacher_id VARCHAR(20) NOT NULL COMMENT '教师ID',
    class_id VARCHAR(20) NULL COMMENT '班级ID',
    semester_id VARCHAR(20) NOT NULL COMMENT '学期ID',
    classroom_id VARCHAR(20) NOT NULL COMMENT '教室ID',
    day_of_week INTEGER NOT NULL COMMENT '星期几',
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程安排表';

CREATE TABLE IF NOT EXISTS grade (
    grade_id VARCHAR(20) PRIMARY KEY COMMENT '成绩ID',
    student_id VARCHAR(20) NOT NULL COMMENT '学号',
    course_id VARCHAR(20) NOT NULL COMMENT '课程ID',
    semester_id VARCHAR(20) NOT NULL COMMENT '学期ID',
    teacher_id VARCHAR(20) NOT NULL COMMENT '教师ID',
    usual_score DECIMAL(5,2) NULL COMMENT '平时成绩',
    exam_score DECIMAL(5,2) NULL COMMENT '考试成绩',
    total_score DECIMAL(5,2) NOT NULL COMMENT '总成绩',
    status ENUM('pending', 'approved', 'rejected') NOT NULL COMMENT '成绩状态',
    is_pass TINYINT(1) NULL COMMENT '是否及格',
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='成绩表';

CREATE TABLE IF NOT EXISTS attendance (
    attendance_id VARCHAR(20) PRIMARY KEY COMMENT '考勤ID',
    student_id VARCHAR(20) NOT NULL COMMENT '学号',
    course_id VARCHAR(20) NOT NULL COMMENT '课程ID',
    semester_id VARCHAR(20) NOT NULL COMMENT '学期ID',
    date DATE NOT NULL COMMENT '考勤日期',
    status ENUM('present', 'absent', 'late', 'leave') NOT NULL COMMENT '考勤状态',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_student_course (student_id, course_id),
    INDEX idx_date (date),
    FOREIGN KEY (student_id) REFERENCES student(student_id),
    FOREIGN KEY (course_id) REFERENCES course(course_id),
    FOREIGN KEY (semester_id) REFERENCES semester(semester_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考勤表';

CREATE TABLE IF NOT EXISTS role (
    role_id VARCHAR(20) PRIMARY KEY COMMENT '角色ID',
    name VARCHAR(50) NOT NULL UNIQUE COMMENT '角色名称',
    description TEXT COMMENT '角色描述',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

CREATE TABLE IF NOT EXISTS permission (
    permission_id VARCHAR(20) PRIMARY KEY COMMENT '权限ID',
    name VARCHAR(50) NOT NULL UNIQUE COMMENT '权限名称',
    code VARCHAR(50) NOT NULL UNIQUE COMMENT '权限代码',
    description TEXT COMMENT '权限描述',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限表';

CREATE TABLE IF NOT EXISTS role_permission (
    role_id VARCHAR(20) NOT NULL COMMENT '角色ID',
    permission_id VARCHAR(20) NOT NULL COMMENT '权限ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (role_id, permission_id),
    INDEX idx_role (role_id),
    INDEX idx_permission (permission_id),
    FOREIGN KEY (role_id) REFERENCES role(role_id),
    FOREIGN KEY (permission_id) REFERENCES permission(permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色权限关联表';

CREATE TABLE IF NOT EXISTS user (
    user_id VARCHAR(20) PRIMARY KEY COMMENT '用户ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(100) NOT NULL COMMENT '密码',
    name VARCHAR(50) NOT NULL COMMENT '姓名',
    role_id VARCHAR(20) NOT NULL COMMENT '角色ID',
    related_id VARCHAR(20) NULL COMMENT '关联ID',
    last_login DATETIME NULL COMMENT '最后登录时间',
    login_ip VARCHAR(50) NULL COMMENT '登录IP',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_role (role_id),
    INDEX idx_related (related_id),
    FOREIGN KEY (role_id) REFERENCES role(role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

CREATE TABLE IF NOT EXISTS student_course_selection (
    selection_id     VARCHAR(20)   NOT NULL COMMENT '选课记录ID',
    student_id       VARCHAR(20)   NOT NULL COMMENT '学号',
    course_id        VARCHAR(20)   NOT NULL COMMENT '课程ID',
    schedule_id      VARCHAR(20)   NOT NULL COMMENT '课程安排ID',
    semester_id      VARCHAR(20)   NOT NULL COMMENT '学期ID',
    status           ENUM('selected', 'dropped', 'completed') NOT NULL COMMENT '选课状态',
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='选课记录表';

CREATE TABLE IF NOT EXISTS teaching_plan (
    plan_id          VARCHAR(20)   NOT NULL COMMENT '教学计划ID',
    major_id         VARCHAR(20)   NOT NULL COMMENT '专业ID',
    course_id        VARCHAR(20)   NOT NULL COMMENT '课程ID',
    semester_type    TINYINT       NOT NULL COMMENT '建议修读学期',
    course_nature    ENUM('compulsory', 'elective_public', 'elective_major') NOT NULL COMMENT '课程性质',
    is_prerequisite  TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '是否为先修课程',
    prerequisite_ids VARCHAR(200)  NULL     COMMENT '先修课程ID列表',
    created_at       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (plan_id),
    UNIQUE KEY uk_major_course (major_id, course_id),
    INDEX idx_major_semester (major_id, semester_type),
    INDEX idx_course (course_id),
    INDEX idx_nature (course_nature),
    CONSTRAINT fk_plan_major FOREIGN KEY (major_id) REFERENCES major(major_id),
    CONSTRAINT fk_plan_course FOREIGN KEY (course_id) REFERENCES course(course_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='教学计划表';

CREATE TABLE IF NOT EXISTS audit_log (
    log_id           BIGINT        NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    user_id          VARCHAR(20)   NULL     COMMENT '操作用户ID',
    username         VARCHAR(50)   NULL     COMMENT '操作用户名',
    operation        VARCHAR(30)   NOT NULL COMMENT '操作类型',
    module           VARCHAR(50)   NOT NULL COMMENT '操作模块',
    target_type      VARCHAR(50)   NULL     COMMENT '操作对象类型',
    target_id        VARCHAR(20)   NULL     COMMENT '操作对象ID',
    detail           JSON          NULL     COMMENT '操作详情',
    ip               VARCHAR(50)   NULL     COMMENT '操作IP地址',
    user_agent       VARCHAR(500)  NULL     COMMENT '客户端User-Agent',
    created_at       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    PRIMARY KEY (log_id),
    INDEX idx_user (user_id),
    INDEX idx_module (module),
    INDEX idx_operation (operation),
    INDEX idx_target (target_type, target_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审计日志表';

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

-- 初始数据
INSERT INTO role (role_id, name, description) VALUES
('1', '系统管理员', '系统最高权限，负责系统配置和管理'),
('2', '教务处管理员', '负责教学计划、课程管理、排课管理等'),
('3', '院系管理员', '负责本院系的学生、教师、课程等管理'),
('4', '教师', '负责课程教学、成绩录入、考勤管理等'),
('5', '学生', '负责课程选择、成绩查询、个人信息管理等');

INSERT INTO permission (permission_id, name, code, description) VALUES
('1', '用户管理', 'system:user:manage', '管理系统用户'),
('2', '角色管理', 'system:role:manage', '管理系统角色'),
('3', '权限管理', 'system:permission:manage', '管理系统权限'),
('4', '系统配置', 'system:config:manage', '配置系统参数'),
('5', '学生信息管理', 'student:info:manage', '管理学生信息'),
('6', '学生注册管理', 'student:register:manage', '管理学生注册'),
('7', '教师信息管理', 'teacher:info:manage', '管理教师信息'),
('8', '课程信息管理', 'course:info:manage', '管理课程信息'),
('9', '教学计划管理', 'course:plan:manage', '管理教学计划'),
('10', '学期设置', 'schedule:semester:manage', '设置学期信息'),
('11', '教室管理', 'schedule:classroom:manage', '管理教室信息'),
('12', '排课管理', 'schedule:arrange:manage', '管理课程安排'),
('13', '成绩录入', 'grade:input:manage', '录入学生成绩'),
('14', '成绩审核', 'grade:audit:manage', '审核学生成绩'),
('15', '考勤记录', 'attendance:record:manage', '记录学生考勤'),
('16', '毕业资格审核', 'graduation:audit:manage', '审核学生毕业资格'),
('17', '选课管理', 'course:selection:manage', '管理学生选课'),
('18', '成绩查询', 'grade:query', '查询个人/课程成绩'),
('19', '课表查询', 'schedule:query', '查询课表信息'),
('20', '考勤查询', 'attendance:query', '查询个人考勤记录');

INSERT INTO role_permission (role_id, permission_id) VALUES
('1', '1'), ('1', '2'), ('1', '3'), ('1', '4'), ('1', '5'), ('1', '6'), ('1', '7'), ('1', '8'), ('1', '9'), ('1', '10'), ('1', '11'), ('1', '12'), ('1', '13'), ('1', '14'), ('1', '15'), ('1', '16'), ('1', '17'), ('1', '18'), ('1', '19'), ('1', '20'),
('2', '5'), ('2', '6'), ('2', '7'), ('2', '8'), ('2', '9'), ('2', '10'), ('2', '11'), ('2', '12'), ('2', '13'), ('2', '14'), ('2', '15'), ('2', '16'), ('2', '17'), ('2', '18'), ('2', '19'), ('2', '20'),
('3', '5'), ('3', '6'), ('3', '7'), ('3', '8'), ('3', '9'), ('3', '13'), ('3', '14'), ('3', '15'), ('3', '16'), ('3', '18'), ('3', '19'),
('4', '8'), ('4', '13'), ('4', '15'), ('4', '18'), ('4', '19'),
('5', '17'), ('5', '18'), ('5', '19'), ('5', '20');

INSERT INTO user (user_id, username, password, name, role_id, related_id) VALUES
('1', 'admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '系统管理员', '1', NULL),
('2', 'jwc', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '教务处管理员', '2', NULL),
('3', 'dept', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '院系管理员', '3', NULL),
('4', 'teacher1', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '张老师', '4', 'T001'),
('5', 'student1', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '李同学', '5', 'S001');

INSERT INTO department (department_id, name, description) VALUES
('D001', '计算机学院', '培养计算机相关专业人才'),
('D002', '电子工程学院', '培养电子工程相关专业人才'),
('D003', '商学院', '培养商业管理相关专业人才');

INSERT INTO major (major_id, name, department_id, description) VALUES
('M001', '计算机科学与技术', 'D001', '培养计算机软件与硬件相关人才'),
('M002', '软件工程', 'D001', '培养软件设计与开发相关人才'),
('M003', '电子信息工程', 'D002', '培养电子信息相关人才'),
('M004', '市场营销', 'D003', '培养市场营销相关人才');

INSERT INTO class (class_id, name, major_id, grade) VALUES
('C001', '计科1班', 'M001', '2021'),
('C002', '计科2班', 'M001', '2021'),
('C003', '软工1班', 'M002', '2021'),
('C004', '电子1班', 'M003', '2021'),
('C005', '营销1班', 'M004', '2021');

INSERT INTO student (student_id, name, gender, birthdate, phone, email, department_id, major_id, class_id, enrollment_date, status) VALUES
('S001', '李同学', 'male', '2003-01-01', '13800138001', 'student1@example.com', 'D001', 'M001', 'C001', '2021-09-01', 'active'),
('S002', '王同学', 'female', '2003-02-01', '13800138002', 'student2@example.com', 'D001', 'M001', 'C001', '2021-09-01', 'active'),
('S003', '张同学', 'male', '2003-03-01', '13800138003', 'student3@example.com', 'D001', 'M002', 'C003', '2021-09-01', 'active');

INSERT INTO teacher (teacher_id, name, gender, birthdate, phone, email, department_id, title, specialty, status) VALUES
('T001', '张老师', 'male', '1980-01-01', '13900139001', 'teacher1@example.com', 'D001', '副教授', '软件工程', 'active'),
('T002', '李老师', 'female', '1985-02-01', '13900139002', 'teacher2@example.com', 'D001', '讲师', '计算机科学', 'active'),
('T003', '王老师', 'male', '1975-03-01', '13900139003', 'teacher3@example.com', 'D002', '教授', '电子工程', 'active');

INSERT INTO course (course_id, name, code, credits, hours, type, department_id, description) VALUES
('CO001', '数据结构', 'CS101', 4.0, 64, 'compulsory', 'D001', '数据结构基础课程'),
('CO002', '算法设计与分析', 'CS102', 3.5, 56, 'compulsory', 'D001', '算法设计与分析课程'),
('CO003', '操作系统', 'CS103', 4.0, 64, 'compulsory', 'D001', '操作系统基础课程'),
('CO004', '计算机网络', 'CS104', 3.5, 56, 'compulsory', 'D001', '计算机网络基础课程'),
('CO005', '数据库原理', 'CS105', 3.5, 56, 'compulsory', 'D001', '数据库原理基础课程'),
('CO006', '人工智能导论', 'CS106', 2.0, 32, 'elective', 'D001', '人工智能通识选修课'),
('CO007', '创业基础', 'GE101', 2.0, 32, 'elective', 'D003', '公共选修课');

INSERT INTO semester (semester_id, name, start_date, end_date, teaching_weeks, status) VALUES
('SEM001', '2023-2024学年第一学期', '2023-09-01', '2024-01-15', 18, 'completed'),
('SEM002', '2023-2024学年第二学期', '2024-02-20', '2024-06-30', 18, 'current'),
('SEM003', '2024-2025学年第一学期', '2024-09-01', '2025-01-15', 18, 'upcoming');

INSERT INTO classroom (classroom_id, name, capacity, building, type, status) VALUES
('CR001', 'A101', 50, 'A楼', 'classroom', 'available'),
('CR002', 'A102', 50, 'A楼', 'classroom', 'available'),
('CR003', 'B101', 100, 'B楼', 'lecture_hall', 'available'),
('CR004', 'C101', 40, 'C楼', 'laboratory', 'available');

INSERT INTO teaching_plan (plan_id, major_id, course_id, semester_type, course_nature, is_prerequisite, prerequisite_ids) VALUES
('TP001', 'M001', 'CO001', 1, 'compulsory', 0, NULL),
('TP002', 'M001', 'CO002', 2, 'compulsory', 1, 'CO001'),
('TP003', 'M001', 'CO003', 3, 'compulsory', 0, NULL),
('TP004', 'M001', 'CO004', 4, 'compulsory', 0, NULL),
('TP005', 'M001', 'CO005', 3, 'compulsory', 0, NULL),
('TP006', 'M001', 'CO006', 5, 'elective_major', 0, NULL),
('TP007', 'M001', 'CO007', 2, 'elective_public', 0, NULL);

INSERT INTO course_schedule (schedule_id, mode, max_students, current_students, course_id, teacher_id, class_id, semester_id, classroom_id, day_of_week, start_period, end_period) VALUES
('SCH001', 'class_based', NULL, 0, 'CO001', 'T001', 'C001', 'SEM002', 'CR001', 1, 1, 2),
('SCH002', 'class_based', NULL, 0, 'CO002', 'T002', 'C001', 'SEM002', 'CR001', 2, 3, 4),
('SCH003', 'class_based', NULL, 0, 'CO003', 'T001', 'C001', 'SEM002', 'CR002', 3, 1, 2),
('SCH004', 'class_based', NULL, 0, 'CO004', 'T002', 'C001', 'SEM002', 'CR002', 4, 3, 4),
('SCH005', 'class_based', NULL, 0, 'CO005', 'T001', 'C001', 'SEM002', 'CR001', 5, 1, 2),
('SCH006', 'open_selection', 120, 0, 'CO006', 'T001', NULL, 'SEM002', 'CR003', 2, 7, 8),
('SCH007', 'open_selection', 150, 0, 'CO007', 'T003', NULL, 'SEM002', 'CR003', 5, 7, 8);

INSERT INTO grade (grade_id, student_id, course_id, semester_id, teacher_id, usual_score, exam_score, total_score, status, is_pass) VALUES
('G001', 'S001', 'CO001', 'SEM001', 'T001', 85.00, 90.00, 88.50, 'approved', 1),
('G002', 'S001', 'CO002', 'SEM001', 'T002', 90.00, 85.00, 86.50, 'approved', 1),
('G003', 'S002', 'CO001', 'SEM001', 'T001', 80.00, 85.00, 83.50, 'approved', 1),
('G004', 'S002', 'CO002', 'SEM001', 'T002', 75.00, 80.00, 78.50, 'approved', 1),
('G005', 'S003', 'CO001', 'SEM001', 'T001', 90.00, 95.00, 93.50, 'approved', 1),
('G006', 'S003', 'CO002', 'SEM001', 'T002', 55.00, 45.00, 48.00, 'pending', NULL);

INSERT INTO attendance (attendance_id, student_id, course_id, semester_id, date, status) VALUES
('A001', 'S001', 'CO001', 'SEM002', '2024-03-01', 'present'),
('A002', 'S001', 'CO001', 'SEM002', '2024-03-08', 'present'),
('A003', 'S002', 'CO001', 'SEM002', '2024-03-01', 'present'),
('A004', 'S002', 'CO001', 'SEM002', '2024-03-08', 'late'),
('A005', 'S003', 'CO001', 'SEM002', '2024-03-01', 'absent');
