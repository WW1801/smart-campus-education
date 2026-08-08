/* P0 student number repair. MySQL 8.x; repeatable and transaction-protected. */
START TRANSACTION;

/* Pre-migration audit: empty, non-13-digit, duplicate. */
SELECT 'before_empty_student_no' AS check_name, COUNT(*) AS count_value
FROM student
WHERE student_no IS NULL OR TRIM(student_no) = '';
SELECT 'before_non_13_digit_student_no' AS check_name, COUNT(*) AS count_value
FROM student
WHERE student_no IS NULL OR TRIM(student_no) NOT REGEXP '^[0-9]{13}$';
SELECT 'before_duplicate_student_no' AS check_name, COUNT(*) AS count_value
FROM (
    SELECT student_no FROM student
    WHERE student_no IS NOT NULL AND TRIM(student_no) <> ''
    GROUP BY student_no HAVING COUNT(*) > 1
) duplicate_numbers;

DROP TEMPORARY TABLE IF EXISTS tmp_student_no_repair_plan;
CREATE TEMPORARY TABLE tmp_student_no_repair_plan (
    student_id VARCHAR(20) NOT NULL PRIMARY KEY,
    generated_student_no CHAR(13) NOT NULL,
    stage_student_no VARCHAR(20) NOT NULL UNIQUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

/* A number is year(4) + department code(3) + major code(3) + yearly major sequence(3). */
INSERT INTO tmp_student_no_repair_plan (student_id, generated_student_no, stage_student_no)
WITH RECURSIVE numbers AS (
    SELECT 1 AS sequence_no
    UNION ALL SELECT sequence_no + 1 FROM numbers WHERE sequence_no < 999
), student_scope AS (
    SELECT student_id,
           COALESCE(YEAR(enrollment_date), YEAR(created_at)) AS enrollment_year,
           LPAD(RIGHT(department_id, 3), 3, '0') AS department_code,
           LPAD(RIGHT(major_id, 3), 3, '0') AS major_code,
           student_no, created_at
    FROM student
), targets AS (
    SELECT *, ROW_NUMBER() OVER (
        PARTITION BY enrollment_year, department_code, major_code ORDER BY created_at, student_id
    ) AS target_order
    FROM student_scope
    WHERE student_no IS NULL
       OR TRIM(student_no) NOT REGEXP '^[0-9]{13}$'
       OR LEFT(student_no, 10) <> CONCAT(enrollment_year, department_code, major_code)
), target_groups AS (
    SELECT DISTINCT enrollment_year, department_code, major_code FROM targets
), free_sequences AS (
    SELECT target_group.enrollment_year, target_group.department_code, target_group.major_code, numbers.sequence_no,
           ROW_NUMBER() OVER (
               PARTITION BY target_group.enrollment_year, target_group.department_code, target_group.major_code
               ORDER BY numbers.sequence_no
           ) AS free_order
    FROM target_groups AS target_group
    JOIN numbers
    LEFT JOIN student AS occupied
      ON occupied.student_no = CONCAT(target_group.enrollment_year, target_group.department_code, target_group.major_code,
                                      LPAD(numbers.sequence_no, 3, '0'))
    WHERE occupied.student_id IS NULL
), planned AS (
    SELECT targets.student_id,
           CONCAT(targets.enrollment_year, targets.department_code, targets.major_code,
                  LPAD(free_sequences.sequence_no, 3, '0')) AS generated_student_no
    FROM targets
    JOIN free_sequences
      ON free_sequences.enrollment_year = targets.enrollment_year
     AND free_sequences.department_code = targets.department_code
     AND free_sequences.major_code = targets.major_code
     AND free_sequences.free_order = targets.target_order
)
SELECT student_id, generated_student_no,
       CONCAT('~', LPAD(ROW_NUMBER() OVER (ORDER BY student_id), 19, '0')) AS stage_student_no
FROM planned;

SELECT student_id, generated_student_no FROM tmp_student_no_repair_plan ORDER BY student_id;
SET @student_no_target_count := (
    SELECT COUNT(*) FROM student
    WHERE student_no IS NULL
       OR TRIM(student_no) NOT REGEXP '^[0-9]{13}$'
       OR LEFT(student_no, 10) <> CONCAT(COALESCE(YEAR(enrollment_date), YEAR(created_at)),
           LPAD(RIGHT(department_id, 3), 3, '0'),
           LPAD(RIGHT(major_id, 3), 3, '0'))
);
SET @student_no_plan_count := (SELECT COUNT(*) FROM tmp_student_no_repair_plan);
SET @student_no_plan_ready := (@student_no_target_count = @student_no_plan_count);
SELECT @student_no_target_count AS target_count, @student_no_plan_count AS plan_count,
       IF(@student_no_plan_ready, 'READY', 'STOP_NO_WRITE') AS migration_status;

/* Two writes avoid transient unique-index collisions. No write occurs unless the plan is complete. */
UPDATE student AS target
JOIN tmp_student_no_repair_plan AS plan ON BINARY plan.student_id = BINARY target.student_id
SET target.student_no = plan.stage_student_no
WHERE @student_no_plan_ready;
UPDATE student AS target
JOIN tmp_student_no_repair_plan AS plan ON BINARY plan.student_id = BINARY target.student_id
SET target.student_no = plan.generated_student_no
WHERE @student_no_plan_ready;

/* Post-migration audit: all three values must be zero. */
SELECT 'after_empty_student_no' AS check_name, COUNT(*) AS count_value
FROM student
WHERE student_no IS NULL OR TRIM(student_no) = '';
SELECT 'after_non_13_digit_student_no' AS check_name, COUNT(*) AS count_value
FROM student
WHERE student_no IS NULL OR TRIM(student_no) NOT REGEXP '^[0-9]{13}$';
SELECT 'after_duplicate_student_no' AS check_name, COUNT(*) AS count_value
FROM (
    SELECT student_no FROM student
    WHERE student_no IS NOT NULL AND TRIM(student_no) <> ''
    GROUP BY student_no HAVING COUNT(*) > 1
) duplicate_numbers;
COMMIT;
