# 校园教务系统开发文档

## 项目简介

单体校园教务系统，提供学生、课程、选课、成绩、考勤、排课、毕业审核和学业预警管理。学业预警按固定规则计算，Agent 只做受控意图识别和结果解释；系统保持既有管理员单角色访问方式，不新增权限体系。

## 技术栈与环境

- 后端：Spring Boot 2.7、Spring Security、MyBatis-Plus、Lombok、JUnit 5、Mockito；Java 编译目标 8。
- 前端：Vue 3、Vuex、Vue Router、Element Plus、Axios、Vite 8；Node.js 18+。
- 基础设施：MySQL 8.x、Redis（可选）；Maven 3.6+。Docker 使用 JDK 17，本地后端按 JDK 8 验证。

## 目录结构

```text
backend/
  db/schema/schema.sql        # 唯一正式建表脚本
  db/seed/seed.sql            # 唯一正式演示数据
  db/migrations/              # 按日期的增量迁移
  db/backup/                  # 不自动执行的历史备份
  main/java/.../controller|service|mapper|entity
frontend/
  router/  views/  store/  utils/request.js
docs/
  campus-education-system.md  # 本主文档
  api/ database/ testing/ archive/
```

历史设计、测试与阶段文档已归档至 `docs/archive/`，不作为当前开发入口。

## 本地启动

1. 复制 `.env.example` 为 `.env`，仅配置本地数据库、Redis、JWT；不要提交真实密钥。
2. 按 [数据库说明](database/database.md) 执行 schema、seed 或选择迁移。
3. 后端：`cd backend; mvn spring-boot:run`，服务为 `http://localhost:8080/api`。
4. 前端：`cd frontend; npm run dev`，开发服务器为 `http://localhost:3000`，`/api` 代理到后端。

## 数据库初始化、迁移与关系

应用不自动初始化数据库：`spring.sql.init.mode=never`。新库依次运行 `backend/db/schema/schema.sql`、`backend/db/seed/seed.sql`；已有库按 [backend/db/README.md](../backend/db/README.md) 的前置条件选择 `migrations/` 脚本。备份目录中的文件绝不自动执行。

学生关联专业、班级；成绩、考勤、毕业审核、预警历史均通过 `student_id` 关联学生；成绩、考勤、选课、排课通过 `course_id`、`semester_id` 关联课程和学期；`academic_warning_record.student_id` 有外键指向 `student.student_id`。业务 ID 保持 MyBatis-Plus `assign_id`。详见 [数据库关系与维护](database/database.md)。

## 调用链

后端：`Controller → Service/ServiceImpl → Mapper（MyBatis-Plus）→ Entity → MySQL → Result`。Controller 解析 HTTP 参数并返回统一 `Result`；Service 承载校验、规则与事务；Mapper 按实体主键和关联字段查询；Entity 与表字段映射。

前端：`router/index.js` 负责懒加载与角色路由守卫；`views/` 展示和交互；`store/index.js` 持久化 `user`、`token`；`utils/request.js` 的 Axios 拦截器附加 Bearer Token，401 自动登出；组件将 `Result.data` 转换为表格、趋势和错误提示状态。

学业预警：`frontend/views/agent/Warning.vue 或 Analysis.vue → utils/request.js → EducationAgentController → EducationAgentServiceImpl → Student/Grade/Attendance/GraduationAudit/AcademicWarningRecord Mapper 或 Service → AcademicWarningRuleEngine → AcademicWarning*DTO → Result → 前端表格、详情、趋势和历史展示`。

DeepSeek：`前端问题 → /api/agent/chat → EducationAgentService → LlmClient → DeepSeekLlmClient → 意图识别或固定指标结果解释 → 固定 Service/Mapper 查询数据库 → Result → 前端展示`。大模型不直接访问数据库、不生成 SQL、不决定风险等级；只可识别意图、选择固定工具和解释结果。失败、超时或不可用时回退关键词匹配，SQL 关键词输入会被拒绝。

## 学业预警规则

- 不及格课程数 `>= 2`：高风险。
- 不及格课程数 `= 1`：中风险。
- `status=absent` 的缺勤次数 `>= 3`：中风险。
- 毕业审核 `status=rejected`：高风险。
- 同时命中时取 `high > medium > low` 的最高等级；风险分按命中规则累计并封顶 100。

正式种子数据提供 S001 无风险、S003 单科不及格、S004 两门不及格、S005 三次缺勤、S006 毕业审核拒绝，以及多学期成绩和 WR001–WR004 预警历史。

## 接口索引与安全

详细的请求方法、完整路径、参数、请求体、JWT、返回字段、成功与失败示例，以及 Postman 操作流程位于 [接口文档](api/agent-api.md)。必测接口：

- `POST /api/login`
- `GET /api/agent/warnings/students`
- `GET /api/agent/warnings/students/{studentId}`
- `GET /api/agent/warnings/students/{studentId}/grade-trend`
- `POST /api/agent/warnings/students/{studentId}/records`
- `GET /api/agent/warnings/students/{studentId}/records`
- `PUT /api/agent/warnings/records/{recordId}/process`
- `POST /api/agent/warnings/records/snapshot`
- `POST /api/agent/chat`
- `GET /api/agent/capabilities`

DeepSeek 只读取环境变量 `DEEPSEEK_API_KEY`；`application.yml` 只保留 `${DEEPSEEK_API_KEY:}` 占位符。不得记录密钥、Authorization 或完整请求头，Git 文件中不得出现真实密钥。

## 已知问题与状态

- 预警历史加载失败：通常是旧开发库尚未执行 `20260728_academic_warning_record.sql` 或外键前置表不存在；新库执行 schema 与 seed 即可。可用 `SHOW TABLES LIKE 'academic_warning_record'` 排查。
- 毕业审核测试：审核意见已统一为“必修课程未全部通过”，对应单元测试已通过。
- Windows 前端构建 EPERM：多由 Vite/Node 占用构建文件导致，关闭占用进程后重试；不通过删除项目文件规避。
- 已修复：B-01 学籍状态字段统一为 `status`；B-02 考勤统计 `courseId` 已改为可选；B-03 考勤统计已读取真实记录；B-04 权限树已持久化；B-05 自动排课已实现冲突检测和明细返回；B-06 教学计划已提供 CRUD。归档文档仅保留历史记录。

## 验收清单

- [x] schema 与 seed 已在 MySQL 8.0.15 隔离验收库执行成功（20 张表）。
- [x] 预警规则、历史记录、趋势数据具备单元测试和固定数据链路。
- [x] 未配置 DeepSeek 密钥时，Agent 问答已验证回退为 `keyword`。
- [x] REST 验收覆盖登录、JWT、预警列表/详情/趋势/历史、Agent、401、自动排课冲突和考勤错误参数。
- [x] `mvn test`、`mvn package -DskipTests`、`npm run build` 成功。
- [x] `git diff --check` 通过，密钥扫描无真实密钥匹配。

测试命令、EPERM 排查与安全扫描方式见 [测试文档](testing/testing.md)。
