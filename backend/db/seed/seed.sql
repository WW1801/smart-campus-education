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
('1', 'admin', '$2a$10$gyoK3D1t9bTxDunbFof36uklOYXBAsEWA65KoRnUS9yJ96uqgt13.', '系统管理员', '1', NULL),
('2', 'jwc', '$2a$10$gyoK3D1t9bTxDunbFof36uklOYXBAsEWA65KoRnUS9yJ96uqgt13.', '教务处管理员', '2', NULL),
('3', 'dept', '$2a$10$gyoK3D1t9bTxDunbFof36uklOYXBAsEWA65KoRnUS9yJ96uqgt13.', '院系管理员', '3', NULL),
('4', 'T001', '$2a$10$gyoK3D1t9bTxDunbFof36uklOYXBAsEWA65KoRnUS9yJ96uqgt13.', '张老师', '4', 'T001'),
('5', '2021001001001', '$2a$10$gyoK3D1t9bTxDunbFof36uklOYXBAsEWA65KoRnUS9yJ96uqgt13.', '李同学', '5', 'S001');

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

INSERT INTO student (student_id, student_no, name, gender, birthdate, phone, email, department_id, major_id, class_id, enrollment_date, status) VALUES
('S001', '2021001001001', '李同学', 'male', '2003-01-01', '13800138001', 'student1@example.com', 'D001', 'M001', 'C001', '2021-09-01', 'active'),
('S002', '2021001001002', '王同学', 'female', '2003-02-01', '13800138002', 'student2@example.com', 'D001', 'M001', 'C001', '2021-09-01', 'active'),
('S003', '2021001002001', '张同学', 'male', '2003-03-01', '13800138003', 'student3@example.com', 'D001', 'M002', 'C003', '2021-09-01', 'active');

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
('SEM003', '2024-2025学年第一学期', '2024-09-01', '2025-01-15', 18, 'upcoming'),
('SEM2026-1', '2026-2027学年第一学期', '2026-09-01', '2027-01-15', 18, 'upcoming'),
('SEM2026-2', '2026-2027学年第二学期', '2027-02-20', '2027-06-30', 18, 'upcoming'),
('SEM2027-1', '2027-2028学年第一学期', '2027-09-01', '2028-01-15', 18, 'upcoming'),
('SEM2027-2', '2027-2028学年第二学期', '2028-02-20', '2028-06-30', 18, 'upcoming'),
('SEM2028-1', '2028-2029学年第一学期', '2028-09-01', '2029-01-15', 18, 'upcoming'),
('SEM2028-2', '2028-2029学年第二学期', '2029-02-20', '2029-06-30', 18, 'upcoming'),
('SEM2029-1', '2029-2030学年第一学期', '2029-09-01', '2030-01-15', 18, 'upcoming'),
('SEM2029-2', '2029-2030学年第二学期', '2030-02-20', '2030-06-30', 18, 'upcoming'),
('SEM2030-1', '2030-2031学年第一学期', '2030-09-01', '2031-01-15', 18, 'upcoming'),
('SEM2030-2', '2030-2031学年第二学期', '2031-02-20', '2031-06-30', 18, 'upcoming');

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
('G006', 'S003', 'CO002', 'SEM001', 'T002', 55.00, 45.00, 48.00, 'submitted', NULL);

INSERT INTO attendance (attendance_id, student_id, course_id, semester_id, date, status) VALUES
('A001', 'S001', 'CO001', 'SEM002', '2024-03-01', 'present'),
('A002', 'S001', 'CO001', 'SEM002', '2024-03-08', 'present'),
('A003', 'S002', 'CO001', 'SEM002', '2024-03-01', 'present'),
('A004', 'S002', 'CO001', 'SEM002', '2024-03-08', 'late'),
('A005', 'S003', 'CO001', 'SEM002', '2024-03-01', 'absent');

-- 学业预警验收数据：S001 无风险；S003 单科不及格；S004 多科不及格；
-- S005 三次缺勤；S006 毕业审核拒绝。所有主键均为应用 assign_id 可接受的字符串。
INSERT INTO student (student_id, student_no, name, gender, birthdate, phone, email, department_id, major_id, class_id, enrollment_date, status) VALUES
('S004', '2021001001003', '赵同学', 'female', '2003-04-01', '13800138004', 'student4@example.com', 'D001', 'M001', 'C001', '2021-09-01', 'active'),
('S005', '2021001001004', '钱同学', 'male', '2003-05-01', '13800138005', 'student5@example.com', 'D001', 'M001', 'C001', '2021-09-01', 'active'),
('S006', '2021001001005', '孙同学', 'female', '2003-06-01', '13800138006', 'student6@example.com', 'D001', 'M001', 'C001', '2021-09-01', 'active');

-- 已审核成绩才会参与风险计算；SEM001 与 SEM002 用于验证跨学期趋势。
INSERT INTO grade (grade_id, student_id, course_id, semester_id, teacher_id, usual_score, exam_score, total_score, status, is_pass) VALUES
('G007', 'S001', 'CO003', 'SEM002', 'T001', 86.00, 88.00, 87.20, 'approved', 1),
('G008', 'S003', 'CO003', 'SEM002', 'T001', 58.00, 50.00, 53.20, 'approved', 0),
('G009', 'S004', 'CO001', 'SEM001', 'T001', 82.00, 80.00, 80.80, 'approved', 1),
('G010', 'S004', 'CO003', 'SEM002', 'T001', 45.00, 42.00, 43.20, 'approved', 0),
('G011', 'S004', 'CO004', 'SEM002', 'T002', 50.00, 46.00, 47.60, 'approved', 0),
('G012', 'S005', 'CO001', 'SEM001', 'T001', 80.00, 82.00, 81.20, 'approved', 1),
('G013', 'S005', 'CO002', 'SEM002', 'T002', 76.00, 78.00, 77.20, 'approved', 1),
('G014', 'S006', 'CO001', 'SEM001', 'T001', 79.00, 81.00, 80.20, 'approved', 1);

INSERT INTO attendance (attendance_id, student_id, course_id, semester_id, date, status) VALUES
('A006', 'S005', 'CO001', 'SEM002', '2024-03-05', 'absent'),
('A007', 'S005', 'CO001', 'SEM002', '2024-03-12', 'absent'),
('A008', 'S005', 'CO002', 'SEM002', '2024-03-19', 'absent');

INSERT INTO graduation_audit (audit_id, student_id, total_credits, required_credits, compulsory_pass, elective_public_credits, elective_major_credits, gpa, status, audit_opinion, degree_granted) VALUES
('GA001', 'S006', 28.00, 32.50, 0, 0.00, 0.00, 2.10, 'rejected', '学分未达标，需完成补修后重新审核', 0);

-- 历史快照用于验证记录分页、处理状态筛选与 student_id 外键关联。
INSERT INTO academic_warning_record (record_id, student_id, semester_id, risk_level, risk_score, risk_reason, triggered_rules, calculated_at, process_status, process_opinion, processed_by, processed_at) VALUES
('WR001', 'S003', 'SEM002', 'medium', 25, '不及格课程数为 1 门', '["GRADE_FAILED_SINGLE"]', '2024-06-30 10:00:00', 'processing', '已通知学生准备补考', 'admin', '2024-07-01 09:00:00'),
('WR002', 'S004', 'SEM002', 'high', 40, '不及格课程数达到 2 门', '["GRADE_FAILED_MULTI"]', '2024-06-30 10:05:00', 'pending', NULL, NULL, NULL),
('WR003', 'S005', 'SEM002', 'medium', 30, '缺勤次数达到 3 次', '["ATTENDANCE_ABSENT_3"]', '2024-06-30 10:10:00', 'completed', '已完成考勤约谈', 'admin', '2024-07-02 14:00:00'),
('WR004', 'S006', NULL, 'high', 40, '毕业审核未通过', '["GRADUATION_REJECTED"]', '2024-06-30 10:15:00', 'pending', NULL, NULL, NULL);
