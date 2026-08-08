# 数据库脚本说明

正式初始化文件只有两份：`schema/schema.sql` 建表，`seed/seed.sql` 演示数据。两者均由开发者手动执行；Spring Boot 配置为 `spring.sql.init.mode=never`，不会自动执行任何 SQL。

## 新数据库

1. 创建空的 `education_system` 数据库，执行 `schema/schema.sql`。
2. 执行 `seed/seed.sql`。
3. 验证：`SHOW TABLES LIKE 'academic_warning_record';`，并执行 `SELECT student_id, risk_level FROM academic_warning_record;`。

`seed.sql` 包含无风险、单科不及格、多科不及格、三次缺勤、毕业审核拒绝、跨学期成绩和预警历史记录，供预警页面、趋势和历史接口验收。

## 已有开发库

先备份数据库，再按实际版本选择迁移，不能把所有迁移重复批量执行：

1. 旧成绩状态为 `pending` 时，执行 `migrations/20260726_grade_status.sql`。
2. `student.student_no` 不存在时，执行 `migrations/20260727_student_no.sql`；若列已存在但为空或重复，改执行 `20260727_student_no_repair.sql`。两者二选一。
3. `academic_warning_record` 不存在时，执行 `migrations/20260728_academic_warning_record.sql`。
4. 考勤表尚未支持 `early` 或缺少日记录唯一约束时，先确认不存在重复数据，再执行 `migrations/20260728_attendance_integrity.sql`。
5. 更早版本才参考 `20260720_legacy_upgrade_v2.sql` 与 `20260720_password_hash_repair.sql`，执行前逐条核对表结构和数据状态。

## 不会自动执行的文件

- `migrations/`：人工、按版本选择执行。
- `backup/`：历史导出和旧脚本，禁止作为初始化输入；其中 `backup_before_student_no.sql` 含历史数据，仅供恢复核对。
- `backup/legacy_create_tables_with_seed.sql`、`backup/legacy_init.sql`：已归档的混合脚本，不是正式入口。

所有业务主键保持 MyBatis-Plus `assign_id`；请勿改为数据库自增主键。`audit_log.log_id` 是既有审计日志自增列，非业务实体主键策略变更。
