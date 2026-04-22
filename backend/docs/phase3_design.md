# Phase 3 设计草稿：选课模块 + 考勤/毕业模块

## 一、选课模块 API 设计草稿

### 1.1 选课流程

```
学生登录 → 查看可选课程(按教学计划过滤) → 选择排课(schedule_id)
→ 系统校验(先修课程+时间冲突+容量) → 创建选课记录 → 更新排课容量
→ 生成个人课表
```

### 1.2 API 设计

| API | 方法 | 说明 | 优先级 |
|-----|------|------|--------|
| /selection/available | GET | 查询可选课程列表（按专业+学期过滤） | P0 |
| /selection | POST | 选课（含冲突检测+容量校验） | P0 |
| /selection/{id}/drop | PUT | 退选 | P0 |
| /selection/my-courses | GET | 查询我的选课列表 | P0 |
| /selection/my-schedule | GET | 查询我的课表 | P0 |
| /selection/page | GET | 管理员查询选课记录 | P1 |

### 1.3 选课核心逻辑

```java
// 选课校验顺序
1. 检查选课时间窗口是否开放
2. 检查学生学籍状态是否为 active
3. 检查是否已选该排课（uk_student_schedule）
4. 检查先修课程是否已通过（teaching_plan.prerequisite_ids）
5. 检查时间冲突（与已选课程比对 day_of_week + start_period~end_period）
6. 检查容量（course_schedule.current_students < max_students）
7. 全部通过 → 创建选课记录 + current_students +1（乐观锁）
```

### 1.4 容量并发安全方案

```sql
-- 乐观锁更新容量
UPDATE course_schedule
SET current_students = current_students + 1
WHERE schedule_id = ?
  AND current_students < max_students;
-- 影响行数=0 表示容量已满
```

### 1.5 前端交互原型描述

- **可选课程页**：按学期展示课程卡片，显示课程名/教师/时间/剩余容量，点击"选课"按钮
- **已选课程页**：列表展示已选课程，支持退选操作
- **我的课表页**：周视图表格，按周一~周日/第1~8节展示课程色块

---

## 二、考勤模块设计

### 2.1 核心字段设计

| 字段 | 类型 | 说明 |
|------|------|------|
| attendance_id | VARCHAR(20) PK | 考勤ID |
| student_id | VARCHAR(20) FK | 学号 |
| schedule_id | VARCHAR(20) FK | 排课ID（替代course_id，更精确） |
| semester_id | VARCHAR(20) FK | 学期ID |
| date | DATE | 考勤日期 |
| status | ENUM | present/absent/late/leave |
| remark | VARCHAR(200) | 备注 |

### 2.2 简化版业务逻辑（当前可落地）

| 功能 | API | 说明 |
|------|-----|------|
| 考勤录入 | POST /attendance | 教师按排课录入单条/批量考勤 |
| 考勤查询 | GET /attendance/page | 按学生/课程/日期查询 |
| 考勤修改 | PUT /attendance | 修改考勤记录 |
| 考勤统计 | GET /attendance/statistics | 按学生/课程统计出勤率 |

### 2.3 后续迭代

- 考勤预警：旷课次数 ≥ 3次自动通知辅导员
- 考勤统计报表：按班级/课程生成出勤率图表

---

## 三、毕业模块设计

### 3.1 核心字段设计

| 字段 | 类型 | 说明 |
|------|------|------|
| audit_id | VARCHAR(20) PK | 审核ID |
| student_id | VARCHAR(20) FK | 学号 |
| total_credits | DECIMAL(5,1) | 已获总学分 |
| required_credits | DECIMAL(5,1) | 要求总学分 |
| compulsory_pass | BOOLEAN | 必修课是否全部通过 |
| elective_public_credits | DECIMAL(5,1) | 公共选修课学分 |
| elective_major_credits | DECIMAL(5,1) | 专业选修课学分 |
| gpa | DECIMAL(3,2) | 绩点 |
| status | ENUM | pending/approved/rejected |
| audit_opinion | TEXT | 审核意见 |

### 3.2 简化版业务逻辑（当前可落地）

| 功能 | API | 说明 |
|------|-----|------|
| 毕业资格审核 | POST /graduation/audit/{studentId} | 自动计算学分+绩点+必修课通过情况 |
| 审核结果查询 | GET /graduation/audit/{studentId} | 查询审核结果 |
| 批量审核 | POST /graduation/batch-audit | 按班级/专业批量审核 |

### 3.3 毕业审核核心SQL

```sql
-- 已获学分
SELECT SUM(c.credits)
FROM grade g
JOIN course c ON g.course_id = c.course_id
WHERE g.student_id = ? AND g.status = 'approved' AND g.is_pass = 1;

-- 必修课是否全部通过
SELECT COUNT(*) AS total,
       SUM(CASE WHEN g.is_pass = 1 THEN 1 ELSE 0 END) AS passed
FROM teaching_plan tp
JOIN grade g ON g.course_id = tp.course_id AND g.student_id = ?
WHERE tp.major_id = ?
  AND tp.course_nature = 'compulsory'
  AND g.status = 'approved';
-- 判定：total = passed 则全部通过
```

### 3.4 后续迭代

- 学位授予流程：毕业审核通过 → 院系推荐 → 教务处授予学位
- 证书管理：毕业证/学位证编号生成、打印、补办
- 就业统计：对接就业系统
