# 选课模块 + 排课冲突检测 测试用例

## 一、选课模块测试用例

### 1.1 选课功能测试

| 用例ID | 测试场景 | 前置条件 | 操作 | 预期结果 | 关联规则 |
|--------|---------|---------|------|---------|---------|
| SEL-001 | 正常选课 | 学生S001在读(active)，排课SCH006为open_selection模式，剩余容量>0 | POST /selection/select {studentId:"S001", scheduleId:"SCH006"} | 返回200，创建选课记录(status=selected)，current_students+1 | 选课流程5.7 |
| SEL-002 | 重复选课 | 学生S001已选SCH006 | POST /selection/select {studentId:"S001", scheduleId:"SCH006"} | 返回错误"已选该课程，不可重复选课" | 唯一约束uk_student_schedule |
| SEL-003 | 非在读学生选课 | 学生S002状态为suspended | POST /selection/select {studentId:"S002", scheduleId:"SCH006"} | 返回错误"当前学籍状态不可选课" | 学籍流转规则5B.3 |
| SEL-004 | 选按班级排课的课程 | 排课SCH001为class_based模式 | POST /selection/select {studentId:"S001", scheduleId:"SCH001"} | 返回错误"该排课为按班级排课模式，不可通过选课加入" | 排课模式4.1.8 |
| SEL-005 | 先修课程未通过 | 算法设计(CO002)先修要求数据结构(CO001)，学生未通过CO001 | POST /selection/select 选CO002的排课 | 返回错误"先修课程未通过：数据结构" | 先修课程逻辑4.1.16 |
| SEL-006 | 先修课程已通过 | 学生S001已通过CO001(grade.status=approved, is_pass=true) | POST /selection/select 选CO002的排课 | 返回200，选课成功 | 先修课程逻辑4.1.16 |
| SEL-007 | 时间冲突选课 | 学生S001已选SCH006(周2第7-8节)，再选同一时段另一排课 | POST /selection/select 选冲突排课 | 返回错误"时间冲突：与已选课程「人工智能导论」冲突（周2第7-8节）" | 排课冲突5B.2 |
| SEL-008 | 容量已满 | 排课max_students=120, current_students=120 | POST /selection/select | 返回错误"选课人数已满，无法选课" | 容量控制4.1.15 |
| SEL-009 | 并发选课容量安全 | 两个学生同时选最后一席 | 并发POST /selection/select | 仅一人成功，另一人返回"选课人数已满" | 乐观锁current_students |

### 1.2 退选功能测试

| 用例ID | 测试场景 | 前置条件 | 操作 | 预期结果 |
|--------|---------|---------|------|---------|
| SEL-010 | 正常退选 | 学生S001已选SCH006(status=selected) | PUT /selection/{id}/drop | 返回200，status变为dropped，current_students-1 |
| SEL-011 | 重复退选 | 选课记录status已为dropped | PUT /selection/{id}/drop | 返回错误"仅已选状态可退选" |
| SEL-012 | 退选后重新选课 | 学生S001已退选SCH006 | POST /selection/select {studentId:"S001", scheduleId:"SCH006"} | 返回200，可重新选课（容量允许时） |

### 1.3 可选课程查询测试

| 用例ID | 测试场景 | 操作 | 预期结果 |
|--------|---------|------|---------|
| SEL-013 | 查询可选课程列表 | GET /selection/available?studentId=S001&semesterId=SEM002 | 返回课程列表，每项含alreadySelected/prerequisiteMet/timeConflict/remaining标记 |
| SEL-014 | 已选课程标记 | 学生S001已选SCH006 | 查询结果中SCH006的alreadySelected=true | 
| SEL-015 | 先修未通过标记 | 学生未通过CO001，CO002要求CO001先修 | CO002的prerequisiteMet=false |
| SEL-016 | 时间冲突标记 | 学生已选周2第7-8节的课程 | 同一时段课程的timeConflict=true |

---

## 二、排课冲突检测测试用例

### 2.1 教师时间冲突（P0）

| 用例ID | 测试场景 | 前置条件 | 操作 | 预期结果 | 优先级 |
|--------|---------|---------|------|---------|-------|
| SCH-001 | 教师同一时段排两门课 | 教师T001在SEM002周1第1-2节已有排课SCH001 | POST /schedule/check-conflict 新排课(T001, 周1, 第1-2节) | 返回冲突type=teacher, priority=P0 | P0(不可调和) |
| SCH-002 | 教师时段部分重叠 | 教师T001在周1第1-2节有排课 | 新排课(T001, 周1, 第2-3节) | 返回冲突(第2节重叠) | P0 |
| SCH-003 | 教师不同天不冲突 | 教师T001在周1有排课 | 新排课(T001, 周2, 第1-2节) | 无冲突 | - |
| SCH-004 | 教师同天不同时段不冲突 | 教师T001在周1第1-2节有排课 | 新排课(T001, 周1, 第3-4节) | 无冲突 | - |

### 2.2 教室时间冲突（P2）

| 用例ID | 测试场景 | 前置条件 | 操作 | 预期结果 | 优先级 |
|--------|---------|---------|------|---------|-------|
| SCH-005 | 教室同一时段排两门课 | 教室CR001在SEM002周1第1-2节已被SCH001占用 | 新排课(不同教师, CR001, 周1, 第1-2节) | 返回冲突type=classroom, priority=P2 | P2(可换教室) |
| SCH-006 | P0+P2同时存在 | 同一教师+同一教室同一时段 | 新排课 | 同时返回P0和P2冲突 | P0优先 |

### 2.3 班级时间冲突（P3）

| 用例ID | 测试场景 | 前置条件 | 操作 | 预期结果 | 优先级 |
|--------|---------|---------|------|---------|-------|
| SCH-007 | 班级同一时段排两门课 | 班级C001在SEM002周1第1-2节已有SCH001 | 新排课(不同教师, C001, 周1, 第1-2节) | 返回冲突type=class, priority=P3 | P3(可调时段) |
| SCH-008 | 开放选课无班级冲突 | 排课mode=open_selection, classId=null | 新排课(classId=null) | 不检测班级冲突 | - |

### 2.4 保存时冲突处理

| 用例ID | 测试场景 | 操作 | 预期结果 |
|--------|---------|------|---------|
| SCH-009 | P0冲突时保存 | POST /schedule 含教师冲突 | 返回错误"排课冲突（不可调和）"，不保存 |
| SCH-010 | 仅P2冲突时保存 | POST /schedule 仅教室冲突 | 保存成功，日志记录P2冲突警告 |
| SCH-011 | 仅P3冲突时保存 | POST /schedule 仅班级冲突 | 保存成功，日志记录P3冲突警告 |
| SCH-012 | 无冲突保存 | POST /schedule 无任何冲突 | 保存成功，无警告 |

### 2.5 编辑排课冲突检测

| 用例ID | 测试场景 | 操作 | 预期结果 |
|--------|---------|------|---------|
| SCH-013 | 编辑自身不冲突 | PUT /schedule 修改SCH001的时间（不与其他排课冲突） | 保存成功 |
| SCH-014 | 编辑时排除自身 | PUT /schedule 检测SCH001冲突时排除SCH001自身 | 不误报自身冲突 |
