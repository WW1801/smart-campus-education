# 校园教务系统 — 本地开发指南

本项目是一个校园教务管理系统，面向学校教务管理、教师教学管理和学生个人教务查询等场景。当前仓库采用**单体 Spring Boot 后端 + Vue 3 管理端 + 微信小程序学生端**结构，主要覆盖用户认证、学生管理、教师管理、课程班级、选课、成绩、考勤、请假、排课、毕业审核、学业预警和 Agent 学业分析等功能。

> 当前项目以本地开发、课程展示和功能演示为主，暂不提供生产上线方案。README 只保留快速了解和启动项目所需内容；完整接口、数据库、Agent、部署和测试细节见 `docs/开发文档.md`。

## 功能概览

| 功能 | 当前状态 | 说明 |
|---|---|---|
| 登录认证 | 已实现 | Spring Security + JWT，除登录接口外默认需要 Bearer Token |
| 用户与角色管理 | 已实现 | 用户、角色、权限维护，角色权限保存仍有待完善点 |
| 学生与教师管理 | 已实现 | 档案维护、账号开通、状态管理 |
| 课程与班级管理 | 已实现 | 院系、专业、班级、课程、学期、教室等基础数据 |
| 选课管理 | 已实现 | 开放选课、退选、我的课程、课表查询 |
| 成绩管理 | 已实现 | 成绩录入、批量录入、审核、统计、GPA |
| 考勤与请假 | 已实现 | 考勤保存、统计、学生请假、审批 |
| 课表与排课 | 已实现/待完善 | 课程安排、冲突检测、规则排课 |
| 毕业审核 | 已实现 | 单人/批量审核、授位、统计、补修建议 |
| 学业预警 | 已实现 | 基于成绩、考勤、毕业审核等规则生成预警 |
| Agent 智能分析 | 已实现/待完善 | 规则分析为主，可选接入 DeepSeek 兼容接口 |
| 微信小程序 | 开发中 | 已有登录、课表、成绩、请假、个人中心等页面结构 |
| Jenkins | 计划完善 | 当前仓库未包含生产级 Jenkinsfile |

## 技术栈

| 类型 | 技术 |
|---|---|
| 后端 | Java 8、Spring Boot 2.7.18、Spring Security、MyBatis-Plus、JWT、Lombok |
| 数据库 | MySQL 8.x |
| 缓存 | Redis 7，当前主要完成依赖和部署配置，业务使用待完善 |
| 前端 | Vue 3、Vite、Vue Router、Vuex、Element Plus、Axios、ECharts |
| 小程序 | 微信小程序原生结构 |
| 测试 | JUnit 5、Mockito、Vitest、Playwright |
| 本地容器联调 | Docker Compose、Nginx、Dockerfile |

## 环境与依赖

| 环境 | 要求 |
|---|---|
| JDK | Java 8，本地编译目标为 1.8 |
| Maven | 3.6+，仓库未提供 Maven Wrapper |
| Node.js | 18+ |
| MySQL | 8.x |
| Redis | 7.x |
| Docker | 支持 Docker Compose |

本地环境变量建议放在 `.env` 或系统环境变量中，不要提交真实值：

```text
SPRING_DATASOURCE_URL=<your-jdbc-url>
SPRING_DATASOURCE_USERNAME=<your-username>
SPRING_DATASOURCE_PASSWORD=<your-password>
SPRING_REDIS_HOST=<your-redis-host>
SPRING_REDIS_PORT=<your-redis-port>
SPRING_REDIS_PASSWORD=<your-password>
JWT_SECRET=<your-secret-at-least-32-bytes>
DEEPSEEK_API_KEY=<your-optional-api-key>
```

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

### 4. Docker Compose 本地联调

## 关键技术栈细节

Docker Compose 用于本地一键联调 MySQL、Redis、后端和 Nginx 前端服务。前端容器读取 `frontend/dist`，首次运行前需要先构建前端：

```powershell
cd frontend
npm run build
```

## 目录结构

```text
.
├── backend/                  # Spring Boot 后端
│   ├── pom.xml               # Maven 配置
│   ├── Dockerfile            # 后端镜像构建
│   ├── db/                   # 数据库脚本
│   │   ├── schema/           # 建表脚本
│   │   ├── seed/             # 初始化演示数据
│   │   ├── migrations/       # 增量迁移脚本
│   │   └── README.md         # 数据库初始化说明
│   ├── main/java/            # 后端源码
│   ├── main/resources/       # application.yml 等配置
│   └── src/test/             # 后端测试
├── frontend/                 # Vue 3 管理端
│   ├── package.json          # 依赖与脚本
│   ├── vite.config.js        # Vite 配置和 /api 代理
│   ├── playwright.config.js  # Playwright E2E 配置
│   └── src/                  # 前端源码
├── miniprogram/              # 微信小程序学生端
├── docs/                     # 开发文档、接口文档、测试文档、原型资料
├── docker-compose.yml        # 容器编排配置
└── nginx.conf                # Nginx 静态资源与 /api 反向代理
```

## 使用示例

### 后端接口地址

```text
http://localhost:8080/api
```

### 前端访问地址

```text
http://localhost:3000
```

### 登录接口

```http
POST /api/login
Content-Type: application/json
```

请求体示例：

```json
{
  "username": "<your-username>",
  "password": "<your-password>"
}
```

统一返回结构：

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

登录成功后，前端会保存 Token，并在后续请求中自动携带：

```http
Authorization: Bearer <your-token>
```

## 常用命令

| 操作 | 命令 |
|---|---|
| 后端启动 | `cd backend; mvn spring-boot:run` |
| 后端测试 | `cd backend; mvn test` |
| 后端打包 | `cd backend; mvn clean package` |
| 前端启动 | `cd frontend; npm run dev` |
| 前端构建 | `cd frontend; npm run build` |
| 前端单元测试 | `cd frontend; npm run test` |
| 前端 E2E 测试 | `cd frontend; npm run test:e2e` |
| Docker 本地联调 | `docker-compose up --build` |

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

- 不要提交 `.env`、真实密码、Token、Cookie、API Key。
- 不要提交 `node_modules/`、`frontend/dist/`、`backend/target/`、日志文件和 Playwright 临时报告。
- 当前项目是单体 Spring Boot 项目，不要描述为已完成的微服务系统。
- 当前暂不面向生产上线；Docker Compose 仅作为本地开发和演示联调用途。
- Jenkins、Redis 深度业务缓存和部分 AI 能力仍是待完善内容。
