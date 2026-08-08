-- 师生登录账号统一迁移：教师使用 teacher.teacher_id（工号），学生使用 student.student_no（学号）。
-- 执行前请先备份数据库；校验失败时不会修改 user 表数据。

DELIMITER //

DROP PROCEDURE IF EXISTS validate_business_login_username //
CREATE PROCEDURE validate_business_login_username()
BEGIN
    DECLARE invalid_count INT DEFAULT 0;

    SELECT COUNT(*) INTO invalid_count
    FROM user u
    WHERE (u.role_id = '4' AND NOT EXISTS (SELECT 1 FROM teacher t WHERE t.teacher_id = u.related_id))
       OR (u.role_id = '5' AND NOT EXISTS (
            SELECT 1 FROM student s
            WHERE s.student_id = u.related_id
              AND s.student_no IS NOT NULL
              AND TRIM(s.student_no) <> ''
       ));
    IF invalid_count > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = '存在未关联有效教师或未生成学号学生的账号，已取消登录账号迁移';
    END IF;

    SELECT COUNT(*) INTO invalid_count
    FROM user existing_user
    INNER JOIN (
        SELECT u.user_id, t.teacher_id AS target_username
        FROM user u
        INNER JOIN teacher t ON t.teacher_id = u.related_id
        WHERE u.role_id = '4'
        UNION ALL
        SELECT u.user_id, s.student_no AS target_username
        FROM user u
        INNER JOIN student s ON s.student_id = u.related_id
        WHERE u.role_id = '5'
    ) target ON target.target_username = existing_user.username
              AND target.user_id <> existing_user.user_id;
    IF invalid_count > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = '目标工号或学号已被其他账号占用，已取消登录账号迁移';
    END IF;
END //

DELIMITER ;

CALL validate_business_login_username();

START TRANSACTION;

-- 先写入迁移占位值，避免两个历史用户名互换时触发唯一索引冲突。
UPDATE user
SET username = CONCAT('__business_login_migrate_', user_id)
WHERE role_id IN ('4', '5');

UPDATE user u
INNER JOIN teacher t ON t.teacher_id = u.related_id
SET u.username = t.teacher_id,
    u.name = t.name
WHERE u.role_id = '4';

UPDATE user u
INNER JOIN student s ON s.student_id = u.related_id
SET u.username = s.student_no,
    u.name = s.name
WHERE u.role_id = '5';

COMMIT;

DROP PROCEDURE IF EXISTS validate_business_login_username;
