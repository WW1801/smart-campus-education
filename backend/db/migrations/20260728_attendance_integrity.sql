-- 统一考勤状态，并按学生、课程、学期、日期保证唯一，支持跨学期同日数据。
-- 兼容早期数据库：旧库可能存在旧唯一索引，也可能从未创建过该索引。
SET @attendance_integrity_ddl = (
    SELECT CASE
        WHEN EXISTS (
            SELECT 1
            FROM information_schema.statistics
            WHERE table_schema = DATABASE()
              AND table_name = 'attendance'
              AND index_name = 'uk_attendance_student_course_semester_date'
        ) THEN
            'ALTER TABLE attendance MODIFY COLUMN status ENUM(''present'', ''absent'', ''late'', ''leave'', ''early'') NOT NULL COMMENT ''考勤状态：present-出勤 absent-旷课 late-迟到 leave-请假 early-早退'''
        WHEN EXISTS (
            SELECT 1
            FROM information_schema.statistics
            WHERE table_schema = DATABASE()
              AND table_name = 'attendance'
              AND index_name = 'uk_attendance_student_course_date'
        ) THEN
            'ALTER TABLE attendance MODIFY COLUMN status ENUM(''present'', ''absent'', ''late'', ''leave'', ''early'') NOT NULL COMMENT ''考勤状态：present-出勤 absent-旷课 late-迟到 leave-请假 early-早退'', DROP INDEX uk_attendance_student_course_date, ADD UNIQUE KEY uk_attendance_student_course_semester_date (student_id, course_id, semester_id, date)'
        ELSE
            'ALTER TABLE attendance MODIFY COLUMN status ENUM(''present'', ''absent'', ''late'', ''leave'', ''early'') NOT NULL COMMENT ''考勤状态：present-出勤 absent-旷课 late-迟到 leave-请假 early-早退'', ADD UNIQUE KEY uk_attendance_student_course_semester_date (student_id, course_id, semester_id, date)'
    END
);

PREPARE attendance_integrity_stmt FROM @attendance_integrity_ddl;
EXECUTE attendance_integrity_stmt;
DEALLOCATE PREPARE attendance_integrity_stmt;
