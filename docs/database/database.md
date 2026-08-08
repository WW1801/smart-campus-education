# 数据库关系与维护

核心关系：`student` 通过 `department_id`、`major_id`、`class_id` 归属组织；`grade`、`attendance`、`graduation_audit`、`academic_warning_record` 均以 `student_id → student.student_id` 关联。`grade`、`attendance`、`student_course_selection`、`course_schedule` 均以 `course_id → course.course_id` 和/或 `semester_id → semester.semester_id` 关联；`course_schedule` 另关联教师、班级和教室。

`academic_warning_record` 的 `fk_warning_student` 外键指向 `student.student_id`，索引 `idx_warning_student(student_id, calculated_at)` 支持学生历史查询。正式 DDL 与种子数据的位置及已有库迁移顺序见 [数据库 README](../../backend/db/README.md)。

不执行 `DROP DATABASE`、`DROP TABLE`、全表 `DELETE`。备份只存放于 `backend/db/backup/`，不会被应用自动加载。
