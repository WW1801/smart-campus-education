# 校园教务系统 — 开发指南

## 快速启动

```powershell
# 后端（需要 MySQL 在 localhost:3306，数据库 education_system）
cd backend; mvn spring-boot:run

# 前端（开发服务器 :3000，代理 /api → :8080）
cd frontend; npm run dev

# Docker 全栈启动（需要 .env 文件配置 MYSQL_ROOT_PASSWORD, REDIS_PASSWORD, JWT_SECRET）
docker-compose up
```

## 数据库配置

- 建表脚本：`backend/db/create_tables.sql`（18 张表）
- 种子数据：`backend/main/resources/init.sql`（演示用户、课程等）
- Spring Boot **不会自动初始化**（`spring.sql.init.mode=never`）；需手动执行两个 SQL 文件或通过 Docker 初始化。
- ID 策略为 `assign_id`（雪花算法）— 不要使用自增主键。

## 项目结构

| 目录 | 角色 | 入口 |
|------|------|------|
| `backend/` | Spring Boot 2.7 + MyBatis Plus + Security | `com.campus.education.EducationSystemApplication` |
| `frontend/` | Vue 3 + Element Plus + Vuex | `main.js` → `App.vue` |
| `docs/` | 设计与测试文档 | `设计与测试文档.md` |

## 关键技术栈细节

- **Java 1.8** 编译目标（pom.xml），但 **Dockerfile 使用 openjdk:17**。本地开发用 JDK 8，Docker 构建用 JDK 17。
- 后端全面使用 **Lombok**（1.18.46）。
- **Vite 8**、**Vue 3.4+**、**Element Plus 2.4+**。
- **后端上下文路径**：`/api`，端口 `8080`。
- **前端开发代理**：`/api` → `http://localhost:8080`（在 `vite.config.js` 中配置）。
- **JWT 认证**：Bearer token，2 小时过期。密钥通过 `JWT_SECRET` 环境变量配置（HS256 至少 32 字符）。
- **MyBatis Plus mapper XML** 路径：`classpath:mapper/**/*.xml`（目录 `backend/main/resources/mapper/`）。

## 常用命令

| 操作 | 命令（从仓库根目录） |
|------|----------------------|
| 运行后端 | `cd backend; mvn spring-boot:run` |
| 构建后端 | `cd backend; mvn clean package` |
| 运行前端 | `cd frontend; npm run dev` |
| 构建前端 | `cd frontend; npm run build` |
| Docker 全栈 | `docker-compose up`（需要 `.env`） |

## 测试

后端测试使用 **JUnit 5 + Mockito**，通过 `ReflectionTestUtils` 注入字段（无 Spring Boot 测试切片注解）。运行：

```powershell
cd backend; mvn test
```

仅 3 个测试文件，位于 `backend/src/test/java/`：
- `JwtUtilsTest` — token 生成/验证
- `StudentAccessGuardTest` — 访问控制
- `GraduationAuditServiceImplTest` — 毕业审核逻辑

前端**无测试配置**（未配置测试运行器）。

## 已知问题（来自 docs/设计与测试文档.md）

| Bug | 模块 | 问题 |
|-----|------|------|
| B-01 | 学生管理 | 学籍状态变更字段不匹配（前端发送 `targetStatus`，后端期望 `status`） |
| B-02 | 考勤管理 | 统计 API 要求 `courseId` 必填 — 应改为可选 |
| B-03 | 考勤管理 | 统计返回硬编码模拟数据 |
| B-04 | 角色管理 | 权限树保存仅为模拟实现 |
| B-05 | 排课管理 | 自动排课为空壳 API |
| B-06 | 教学计划 | 无后端 CRUD API |

## 环境要求

- `.env` 文件（从 `.env.example` 复制），配置 `MYSQL_ROOT_PASSWORD`、`REDIS_PASSWORD`、`JWT_SECRET`
- MySQL 8.x 在 `localhost:3306`（或通过 Docker）
- Redis（可选 — 可不启动，但 Spring Boot 会尝试连接）
- Maven 3.6+（无 wrapper），Node.js 18+
- 本地开发：JDK 8；Docker 构建：JDK 17

## 开发规范

- RESTful JSON API，统一响应格式 `{ code, message, data }`
- 实体和 DTO 使用 Lombok `@Data` / `@Builder`
- 后端 controller 按模块组织在 `com.campus.education.controller.*` 下
- 前端视图按模块分组在 `frontend/views/` 下
- 路由使用懒加载组件，带角色守卫（`roles: ['1','2',...]`）
- Vuex store 将 `user` 和 `token` 持久化到 `localStorage`
- Axios 拦截器自动附加 `Bearer` token，401 时自动登出

## 架构说明

这是**单模块 Maven 项目**（非微服务，尽管文档提到 Spring Cloud）。根目录的 `src/` 和 `scripts/` 为空 — 所有源码位于 `backend/` 和 `frontend/` 下。

后端关键包：
- `controller/` — 12 个 REST 控制器（约 71 个端点）
- `service/` — 业务逻辑
- `mapper/` — MyBatis Plus 接口
- `entity/` — 18+ 实体类
- `common/` — JWT 工具、操作日志切面、访问守卫
- `config/` — Spring Security、MyBatis Plus 配置
- `dto/` — 请求/响应对象
