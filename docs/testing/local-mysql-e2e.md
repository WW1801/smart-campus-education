# 本地 MySQL 端到端验收

在启动后端的同一终端设置数据库环境变量，并先确认数据库连接：

```powershell
$env:SPRING_DATASOURCE_USERNAME='root'
$env:SPRING_DATASOURCE_PASSWORD='你的数据库密码'
& 'D:\MySQL\mysql-8.0.15-winx64\bin\mysql.exe' -h 127.0.0.1 -P 3306 -u root -p -e 'SELECT 1'
```

空库依次执行 `backend/db/schema/schema.sql`、`backend/db/seed/seed.sql`；已有库仅按 `backend/db/README.md` 的前置条件选择迁移，禁止重复导入全量脚本。

启动 `cd backend; mvn spring-boot:run` 后，依次验收：登录、`GET /api/attendance/roster`、`POST /api/attendance/batch-save`、`POST /api/schedule-basic/auto-arrange`、`GET /api/schedule/page`。确认批量考勤和自动排课产生的数据均能被查询到。
