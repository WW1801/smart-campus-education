# 发布变更审查

审查日期：2026-07-30。本文仅记录当前工作区状态；未删除、恢复、移动或暂存任何文件。

## 待提交文件分组

| 分组 | 状态 | 主要文件或目录 | 发布审查结论 |
|---|---|---|---|
| 请假与考勤闭环 | 新增、修改 | `backend/main/java/com/campus/education/{controller/leave,dto/leave,entity/LeaveRequest.java,mapper/LeaveRequestMapper.java,service/LeaveRequestService.java,service/impl/LeaveRequestServiceImpl.java}`、`Attendance*`、`Result.java`、`SecurityConfig.java` | 应与数据库迁移、前端页面和测试作为一个原子提交发布 |
| 正式数据库资产 | 新增 | `backend/db/schema/schema.sql`、`backend/db/seed/seed.sql`、`backend/db/migrations/*.sql`、`backend/db/README.md` | 空库使用 schema 后 seed；已有库只能按现状选择迁移，禁止整库重放 |
| 请假前端与权限路由 | 新增、修改 | `frontend/views/leave/{My,Approval}.vue`、`frontend/router/index.js`、导航配置、`Home.vue` | 依赖后端请假接口和 attendance 新字段 |
| 自动排课、批量考勤及既有业务增强 | 新增、修改 | `Schedule*`、`Attendance*`、学生/教师/成绩/账号/预警相关控制器与服务、对应页面 | 属于当前工作区既有修改；合并前应按业务域拆分复核 |
| 自动化测试 | 新增、修改 | `backend/src/test/**`、`frontend/tests/**`、`frontend/playwright.config.js` | 测试数据使用 mock 或事务隔离，不依赖生产数据 |
| 配置与依赖 | 修改 | `.env.example`、`application.yml`、`docker-compose.yml`、前端 package 文件、Vite 配置 | 部署端必须显式注入数据库、Redis、JWT 环境变量 |
| 测试与发布文档 | 新增 | `docs/testing/mysql-e2e-checklist.md`、本文件及其他 `docs/**` | checklist 是人工验收说明，不会自动执行数据库脚本 |
| 设计资料与原型 | 新增、归档 | `.impeccable/**`、`DESIGN.md`、`PRODUCT.md`、`docs/prototypes/**`、`docs/archive/**` | 原型、生成 HTML、JFR 与业务代码应分组提交；归档文件不参与运行 |
| 数据库备份 | 新增、归档 | `backend/db/backup/**` | 仅作为历史参考，不得由应用或部署流程自动执行 |

## 当前标记为删除的跟踪文件

下列删除状态在本次任务开始前已存在，本次未执行删除，也未恢复；提交前必须由维护者逐项确认其历史内容已被正式文件或归档替代：

- `backend/db/create_tables.sql`
- `backend/db/fix_passwords.sql`
- `backend/db/upgrade_v2.sql`
- `backend/main/resources/init.sql`
- `backend/docs/business_rules.md`
- `backend/docs/development_plan.md`
- `backend/docs/phase2_design.md`
- `backend/docs/phase3_design.md`
- `backend/docs/test_cases_selection_schedule.md`
- `docs/设计与测试文档.md`

风险：若直接提交这些删除，而部署脚本、运维手册或外部流水线仍引用旧路径，会导致初始化或文档链接失效。

## 数据库迁移适用前置条件

| 迁移 | 适用前置条件与选择规则 |
|---|---|
| `20260720_legacy_upgrade_v2.sql` | 仅适用于由旧 `create_tables.sql` 建立、且尚无选课/教学计划/审计结构的早期库；新版正式 schema 或已包含目标列/表的库不得执行 |
| `20260720_password_hash_repair.sql` | 仅适用于明确隔离的历史演示数据；按固定用户 ID 更新口令哈希，不得用于未知数据或生产库 |
| `20260726_grade_status.sql` | `grade.status` 仍含历史 `pending` 且尚未扩展为 `draft/submitted/approved/rejected` 时执行 |
| `20260727_student_no.sql` | `student.student_no` 不存在时执行；先审计生成规则和重复值 |
| `20260727_student_no_repair.sql` | 列已存在但历史学号缺失、重复或不符合旧生成规则时执行；与新增列迁移按库现状选择，不得盲目重复 |
| `20260728_student_no_format.sql` | `student_no` 已存在，需修复为 13 位格式时执行；要求 MySQL 8，先查看脚本内审计结果和备份 |
| `20260728_academic_warning_record.sql` | 学业预警历史表不存在时执行；目标表已存在时只核对结构，不重复建表 |
| `20260728_attendance_integrity.sql` | attendance 尚未支持 `early`，且仍使用旧唯一键时执行；执行前必须清理会违反新四字段唯一键的重复记录 |
| `20260729_business_login_username.sql` | 学号修复完成，师生 `related_id` 关系完整，且需要切换为工号/学号登录时执行；脚本校验失败必须停止 |
| `20260730_leave_request.sql` | `user/student/course/semester/attendance` 已存在，attendance 四字段唯一键已建立，且尚无 `leave_request` 表和来源字段时执行；需与本次后端同时发布 |

所有迁移均应先在数据库副本演练并记录 schema 版本。除带显式保护的语句外，不应假定脚本可重复执行。

## 配置与敏感信息审查

- `.env.example` 仅保留空环境变量名，不含默认密码、JWT、Token 或外部模型密钥。
- `backend/main/resources/application.yml` 通过环境变量取数据库、Redis、JWT 与 DeepSeek 配置，敏感项没有可用默认值。
- `docker-compose.yml` 与 Spring 本地配置统一使用 `SPRING_DATASOURCE_*`、`SPRING_REDIS_*`、`JWT_SECRET`、`DEEPSEEK_API_KEY`；数据库、Redis 和 JWT 必填。
- Git 跟踪文件扫描未发现疑似明文 JWT、Token、DeepSeek Key 或真实账号密码。测试中的虚构口令和 seed 中不可逆哈希不作为生产凭据。

## 发布与兼容性风险

1. 必须先执行适用的 attendance 完整性迁移及 `20260730_leave_request.sql`，再部署使用新列的后端；反序会导致 SQL 列不存在。
2. 新迁移使用 MySQL 8 的生成列、检查约束或窗口函数能力；低版本 MySQL 未验证。
3. 请假重复保护覆盖相同指纹和业务层时间重叠检查；极端并发下，不同指纹但日期重叠的两次提交仍依赖事务与最终审批冲突控制。
4. Docker 不再接受隐式敏感默认值；旧部署若未注入同名环境变量将启动失败，这是预期的安全性变更。
5. 当前环境没有可确认的隔离 MySQL 实例，因此未执行真实数据库写入验收；发布前须按 `docs/testing/mysql-e2e-checklist.md` 完成。
6. 当前工作区包含大量既有未跟踪文件和十个删除标记；应分组暂存并人工核对，避免把原型、归档或历史删除意外混入发布提交。
