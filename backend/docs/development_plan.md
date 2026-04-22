# 校园教务系统 分阶段开发方案

## 开发任务总览

| 阶段 | 模块 | 优先级 | 预估工时 | 依赖 |
|------|------|--------|---------|------|
| P1-1 | 核心数据层（建表+初始化） | P0 | 0.5d | 无 |
| P1-2 | 通用基础设施（统一响应/JWT/异常处理） | P0 | 1d | P1-1 |
| P1-3 | 系统管理（用户/角色/权限+RBAC） | P0 | 3d | P1-2 |
| P1-4 | 学生管理（CRUD+学籍流转） | P0 | 2d | P1-2 |
| P1-5 | 成绩管理（录入/审核/绩点计算） | P0 | 3d | P1-2, P1-4 |
| P1-6 | 前端页面（系统/学生/成绩） | P0 | 4d | P1-3~5 |
| P1-7 | Docker部署 | P1 | 0.5d | P1-6 |
| P2-1 | 排课管理设计 | P1 | 2d | P1-5 |
| P2-2 | 教学计划管理设计 | P1 | 1d | P1-4 |
| P3-1 | 选课模块API设计 | P2 | 1d | P2-1 |
| P3-2 | 考勤/毕业模块设计 | P2 | 1d | P1-5 |

---

## Phase 1：优先开发（启动阶段）

### P1-1 核心数据层 ✅ 已完成

- [create_tables.sql](file:///d:/work%20spase/trae/2/backend/db/create_tables.sql) - 完整建表脚本（17张表+索引+外键+注释）
- [init.sql](file:///d:/work%20spase/trae/2/backend/main/resources/init.sql) - Spring Boot初始化脚本
- [upgrade_v2.sql](file:///d:/work%20spase/trae/2/backend/db/upgrade_v2.sql) - 增量升级脚本

**关联补充点**：新增 student_course_selection / teaching_plan / audit_log 三张表；grade.status 改为 pending/approved/rejected；course_schedule 增加 mode/max_students/current_students。

### P1-2 通用基础设施

| 子任务 | 说明 | 关联规则 |
|--------|------|---------|
| 统一响应体 Result<T> | code/message/data 三段式 | 全局 |
| 全局异常处理 | @ControllerAdvice + 自定义异常 | 全局 |
| JWT认证过滤器 | 替换当前 permitAll 配置 | 安全需求3.2 |
| 审计日志AOP | @Aspect 切面自动记录操作日志 | 审计日志实体4.1.17 |
| MyBatis-Plus分页配置 | PaginationInnerInterceptor | 性能需求3.1 |

### P1-3 系统管理模块

| 子任务 | API | 关联规则 |
|--------|-----|---------|
| 用户CRUD | GET/POST/PUT/DELETE /system/user | RBAC |
| 角色CRUD+权限分配 | /system/role + /system/role/{id}/permissions | 角色权限多对多4.2 |
| 权限CRUD | /system/permission | 权限代码(code)用于接口鉴权 |
| 登录/登出 | POST /login + POST /logout | JWT+BCrypt |
| 获取当前用户信息+权限 | GET /current-user | 前端菜单动态渲染 |

### P1-4 学生管理模块

| 子任务 | API | 关联规则 |
|--------|-----|---------|
| 学生分页查询 | GET /student/page | 多条件筛选 |
| 学生CRUD | POST/PUT/DELETE /student | 基础操作 |
| 学籍状态变更 | PUT /student/{id}/status | 学籍流转规则5B.3 |
| 学籍变更记录查询 | GET /student/{id}/status-history | 审计日志 |

**关联规则点**：active→suspended需院系审核；active→graduated需满足毕业条件；dropped不可直接恢复。

### P1-5 成绩管理模块

| 子任务 | API | 关联规则 |
|--------|-----|---------|
| 成绩录入（单条/批量） | POST /grade + POST /grade/batch | 成绩计算规则5B.1 |
| 成绩审核（通过/驳回） | PUT /grade/{id}/approve + /reject | 审核流程5.6 |
| 成绩查询（学生/课程/学期） | GET /grade/page | 多维度查询 |
| 绩点计算 | GET /grade/gpa/{studentId} | GPA公式5B.1 |
| 成绩统计 | GET /grade/statistics | 按课程/班级统计 |

**关联规则点**：必修30%+70%；审核通过后自动判定is_pass；绩点=(score-50)/10。

### P1-6 前端页面

| 子任务 | 页面 | 说明 |
|--------|------|------|
| 登录页改造 | Login.vue | 对接真实JWT登录 |
| 用户管理页 | system/User.vue | 表格+弹窗CRUD |
| 角色管理页 | system/Role.vue | 角色CRUD+权限分配树 |
| 权限管理页 | system/Permission.vue | 权限CRUD |
| 学生信息页 | student/Info.vue | 分页表格+高级搜索+CRUD |
| 学籍管理页 | student/Register.vue | 学籍状态变更+审核 |
| 成绩录入页 | grade/Input.vue | 按课程/班级批量录入 |
| 成绩查询页 | grade/Query.vue | 多维度查询+绩点展示 |
| 路由守卫 | router/index.js | 基于权限的动态路由 |

### P1-7 Docker部署

| 文件 | 说明 |
|------|------|
| docker-compose.yml | MySQL + Redis + 后端 + Nginx(前端) |
| Dockerfile (后端) | 基于openjdk:17 |
| nginx.conf | 前端静态资源+API代理 |

---

## Phase 2：设计阶段同步完善

### P2-1 排课管理模块设计

| 设计项 | 说明 | 当前可落地 | 后续迭代 |
|--------|------|-----------|---------|
| 手动排课 | 教务处手动创建/调整排课 | ✅ | - |
| 冲突检测 | 三维度实时检测 | ✅ | - |
| 自动排课算法 | 约束满足问题(CSP) | ❌ 仅伪代码 | 遗传算法/回溯搜索 |
| 课表查询 | 按教师/班级/教室 | ✅ | - |
| 教室推荐 | 冲突时推荐替代教室 | ❌ | 智能推荐 |

### P2-2 教学计划模块设计

| 设计项 | 说明 | 当前可落地 | 后续迭代 |
|--------|------|-----------|---------|
| 教学计划CRUD | 按专业管理课程设置 | ✅ | - |
| 先修课程校验 | 选课时检查先修 | ✅ | - |
| 培养方案版本管理 | 方案变更留痕 | ❌ | 版本控制 |
| 计划变更审批流 | 院系→教务处审批 | ❌ | 工作流引擎 |

---

## Phase 3：开发过程迭代补充

### P3-1 选课模块

| 设计项 | 说明 | 当前可落地 | 后续迭代 |
|--------|------|-----------|---------|
| 选课API草稿 | 选课/退选/查询 | ✅ API设计 | - |
| 容量控制 | current_students并发安全 | ✅ 乐观锁 | - |
| 时间冲突检测 | 与已选课程比对 | ✅ | - |
| 选课时间窗口 | 配置选课起止时间 | ❌ | 系统配置 |
| 抽签机制 | 超额选课随机抽签 | ❌ | 抽签算法 |

### P3-2 考勤/毕业模块

| 设计项 | 说明 | 当前可落地 | 后续迭代 |
|--------|------|-----------|---------|
| 考勤CRUD | 教师记录考勤 | ✅ | - |
| 考勤统计 | 按学生/课程统计 | ✅ | - |
| 考勤预警 | 旷课次数过多预警 | ❌ | 预警规则 |
| 毕业资格审核 | 学分+必修课+绩点 | ✅ 基础版 | - |
| 学位授予 | 审核通过后授予 | ❌ | 证书管理 |
