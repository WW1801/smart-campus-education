# MySQL 端到端验收清单

本清单只用于人工验收。Spring Boot 保持 `spring.sql.init.mode=never`；不得让应用自动执行 schema、seed 或迁移，也不得在来源不明或生产数据库上运行初始化、批量排课、批量考勤、请假审批和账号批处理。

## 1. 验收数据库与环境变量

1. 创建明确隔离、允许写入的测试实例或测试库，并记录负责人、主机、库名和清理方式。连接后先只读确认：

   ```sql
   SELECT DATABASE(), @@hostname, @@port, CURRENT_USER();
   SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE();
   ```

2. 在启动后端的同一个 PowerShell 会话注入变量。尖括号内容由验收人员现场填写，不写入仓库或测试输出：

   ```powershell
   $env:SPRING_DATASOURCE_URL='jdbc:mysql://<host>:<port>/<isolated_test_db>?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai'
   $env:SPRING_DATASOURCE_USERNAME='<test_user>'
   $env:SPRING_DATASOURCE_PASSWORD='<test_password>'
   $env:JWT_SECRET='<at-least-32-byte-random-secret>'
   $env:SPRING_REDIS_HOST='<redis_host>'
   $env:SPRING_REDIS_PORT='<redis_port>'
   $env:SPRING_REDIS_PASSWORD='<redis_password-if-required>'
   ```

3. 不在命令中回显变量值。只验证是否已设置：

   ```powershell
   'SPRING_DATASOURCE_URL','SPRING_DATASOURCE_USERNAME','SPRING_DATASOURCE_PASSWORD','JWT_SECRET' |
     ForEach-Object { "$_=" + [bool](Get-Item "Env:$_" -ErrorAction SilentlyContinue) }
   ```

## 2. 初始化与迁移选择

### 2.1 空库

严格按以下顺序人工执行，且只执行一次：

1. `backend/db/schema/schema.sql`：创建正式结构，预期包含 `leave_request`，`attendance` 包含 `record_source/source_request_id/approved_by/synced_at/manual_locked`。
2. `backend/db/seed/seed.sql`：仅在隔离演示/测试库导入可重复验收所需基础数据。
3. 只读校验：

   ```sql
   SHOW TABLES;
   SHOW CREATE TABLE leave_request;
   SHOW CREATE TABLE attendance;
   SELECT COUNT(*) FROM student;
   SELECT COUNT(*) FROM user;
   ```

预期：建表和种子数据成功；应用启动日志不出现自动 DDL/DML；`attendance` 的业务唯一键为 `(student_id, course_id, semester_id, date)`。

### 2.2 已有库

先备份并只读检查 `information_schema`，按真实缺口选择迁移，禁止把整个目录重复执行：

| 迁移 | 仅在此前置条件满足时执行 |
|---|---|
| `20260720_legacy_upgrade_v2.sql` | 仅早期 `create_tables.sql` 结构，缺少选课、教学计划、审计表及对应列；逐条对照后执行。 |
| `20260720_password_hash_repair.sql` | 仅历史演示账号密码哈希损坏的隔离演示库；会更新固定用户，禁止用于生产。 |
| `20260726_grade_status.sql` | `grade.status` 仍含历史 `pending`，尚未拆分为 `draft/submitted/approved/rejected`。 |
| `20260727_student_no.sql` | `student.student_no` 列不存在；执行前确认学号生成所需字段完整。 |
| `20260727_student_no_repair.sql` | 列已存在但数据为空/重复；与上一条二选一。 |
| `20260728_student_no_format.sql` | 需要把既有学号修复为当前 13 位规则；先审查脚本内审计查询结果。 |
| `20260728_academic_warning_record.sql` | `academic_warning_record` 表不存在。 |
| `20260728_attendance_integrity.sql` | 考勤尚不支持 `early` 或缺少四字段唯一键；执行前先消除重复键。 |
| `20260729_business_login_username.sql` | 师生账号尚未统一为工号/业务学号，且脚本内关联校验能够通过。 |
| `20260730_leave_request.sql` | 已有 `student/course/semester/user/attendance`，已完成考勤完整性迁移，且 `leave_request` 和五个考勤来源列均不存在。 |

执行 `20260730_leave_request.sql` 后只读确认：活动申请唯一指纹、请假筛选索引、考勤来源索引均存在，既有考勤均保持 `record_source=manual`，业务行数不变。

## 3. 启动与通用响应检查

1. `cd backend; mvn spring-boot:run`，再启动前端。
2. 每个 API 响应均检查 `{ code, message, data }`；认证头为 `Authorization: Bearer <token>`，Token 不写入日志或文档。
3. 业务校验失败应返回明确的 `4xx` 业务码/明细；非预期系统异常返回统一错误且对应事务无部分写入。

## 4. 业务验收步骤

### 4.1 登录

1. `POST /api/login`，参数：`username`、`password`。
2. 正确凭据预期：`code=200`；`data.token`、`data.user.userId/username/name/roleId/relatedId/permissions` 存在；`user.last_login/login_ip` 更新。
3. 错误密码预期：`code=401`，无 Token，`last_login/login_ip` 不变。
4. 无 Token 访问受限接口预期 HTTP/业务码 401；学生访问管理员请假分页、管理员代学生创建请假预期 403。

### 4.2 自动排课

1. `POST /api/schedule-basic/auto-arrange`，参数：`semesterId`、`classroomIds[]`、`timeSlots[{dayOfWeek,startPeriod,endPeriod}]`、`tasks[{courseId,teacherId,classId,mode,maxStudents}]`。
2. 成功预期：`data.arrangedCount/failedCount/successDetails/failureDetails`；`course_schedule` 新增对应行，教师、班级、教室、学期和节次匹配；随后 `GET /api/schedule/page` 可查到。
3. 构造教师/教室/班级冲突。预期失败明细含 `index/type/priority/reason/suggestion`，失败任务不写 `course_schedule`，已成功任务只写一次。
4. 原请求重试：已完成任务不重复插入；修改候选时间后失败任务可成功。

### 4.3 批量考勤

1. `GET /api/attendance/roster?courseId=<id>&semesterId=<id>&date=<yyyy-MM-dd>` 获取真实花名册。
2. `POST /api/attendance/batch-save`，参数：`courseId`、`semesterId`、`date`、`records[{studentId,status}]`。
3. 成功预期：`data.successCount/failedCount/successDetails/failureDetails`；`attendance` 按四字段唯一键新增或更新，人工行 `record_source=manual`。
4. 混入无效学生/状态/非花名册学生。预期有效行提交，失败行返回 `studentId/studentNo/studentName/field/reason/suggestion`。
5. 重复提交同一业务键预期更新同一行，不增加重复记录。`manual_locked=1` 或 `record_source=leave_request` 的记录必须明确冲突且不被覆盖/删除。

### 4.4 请假闭环

1. 学生 `POST /api/leave-requests`，参数：`studentId`（可省略，若提供必须等于当前学生）、`courseId`、`semesterId`、`startDate`、`endDate`、`leaveType`、`reason`。
2. 预期 `leave_request` 新增一行：`status=pending`、`attendance_sync_status=pending`、`submitted_at` 有值；不新增考勤。相同活动申请重复提交预期 409，行数不变。
3. `GET /api/leave-requests/my?page=1&limit=10&status=pending` 仅返回当前学生；返回记录含申请字段及学生/课程/学期展示字段。
4. `PUT /api/leave-requests/{requestId}/cancel`：待审批申请变为 `cancelled/not_required`；已通过、已拒绝或他人申请均失败，考勤不变。
5. 管理员 `GET /api/leave-requests/page`，可传 `studentId/courseId/semesterId/status/startDate/endDate/page/limit`；日期条件按申请区间相交筛选。
6. `PUT /api/leave-requests/{requestId}/approve`，参数 `processOpinion`：
   - 成功：申请变为 `approved/synced`，写入 `processed_by/processed_at/process_opinion`；日期范围内每一天恰有一条 `attendance.status=leave`，并写入 `record_source=leave_request/source_request_id/approved_by/synced_at`。
   - 再次批准：`data.idempotent=true`，申请和考勤行数不增加。
   - 预置非请假或人工锁定考勤：返回 409；`data.failureDetails[]` 含 `requestId/date/field/reason/suggestion`；申请保持 `pending`、同步状态为 `failed`，无日期被部分写入。
7. `PUT /api/leave-requests/{requestId}/reject`：仅待审批可变为 `rejected/not_required`，不写考勤。
8. `PUT /api/leave-requests/batch-approve`，参数 `requestIds[]/processOpinion`：返回 `successCount/failedCount/results[]`；业务冲突逐申请返回，系统异常时整批事务回滚。

### 4.5 考勤统计与学业预警

1. 审批前后分别调用 `GET /api/attendance/statistics?semesterId=<id>&courseId=<id>`。
2. 预期返回 `totalCount/presentCount/lateCount/absentCount/leaveCount/earlyCount/attendanceRate/statusData/trend`；批准后 `leaveCount` 与总数按新增日期增加。
3. 调用学业预警快照/学生预警接口。预警规则只把 `attendance.status=absent` 计入缺勤，不把批准后的 `leave` 误判为缺勤；历史 `academic_warning_record` 写入只发生在显式快照操作。

### 4.6 账号生命周期

1. 通过现有账号开通/批量开通接口创建师生账号；预期 `user` 以业务工号/学号为用户名，密码只保存 BCrypt 哈希，重复开通返回已有/跳过明细而不重复写入。
2. 使用 `PUT /api/account/password` 校验旧密码并修改；预期正确旧密码更新哈希，错误旧密码不写入。
3. 将学生改为退学/毕业或教师改为离职，再使用旧 Token 调业务接口；预期 403，账号不能继续办理业务。
4. 恢复有效人员状态后重新登录；预期按角色恢复授权，历史业务数据不删除。

## 5. 事务与收尾核对

1. 对自动排课、批量考勤、请假批量审批分别制造一次数据库异常；预期对应事务无部分系统写入。
2. 查询四字段考勤重复数必须为 0；查询同一 `source_request_id/date` 重复数必须为 0。
3. 保存 API 响应摘要、行数和业务 ID，不保存 Token、密码、JWT 密钥或数据库连接秘密。
4. 隔离库按预先记录的方式整体回收；不得对未知库执行清空、初始化或批量修复。
