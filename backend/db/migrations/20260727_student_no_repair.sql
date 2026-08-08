UPDATE student AS target
JOIN (
    SELECT student_id, CONCAT(
        enrollment_year,
        LPAD(RIGHT(department_id, 3), 3, '0'),
        LPAD(RIGHT(major_id, 3), 3, '0'),
        LPAD(RIGHT(class_id, 3), 3, '0'),
        LPAD(sequence_no, 3, '0')
    ) AS generated_student_no
    FROM (
        SELECT student_id, YEAR(enrollment_date) AS enrollment_year,
               department_id, major_id, class_id,
               ROW_NUMBER() OVER (
                   PARTITION BY YEAR(enrollment_date), department_id, major_id, class_id
                   ORDER BY created_at, student_id
               ) AS sequence_no
        FROM student
    ) AS ranked_student
) AS student_numbers ON student_numbers.student_id = target.student_id
SET target.student_no = student_numbers.generated_student_no;

ALTER TABLE student MODIFY COLUMN student_no VARCHAR(20) NOT NULL;
ALTER TABLE student ADD UNIQUE KEY uk_student_no (student_no);
