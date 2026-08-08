-- 成绩状态拆分：历史 pending 表示教师已提交、等待审核。
ALTER TABLE grade
    MODIFY COLUMN status ENUM('draft', 'submitted', 'approved', 'rejected') NOT NULL COMMENT '成绩状态：draft-待录入 submitted-待审核 approved-审核通过 rejected-审核驳回';

UPDATE grade
SET status = 'submitted'
WHERE status = 'pending';
