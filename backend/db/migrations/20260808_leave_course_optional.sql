-- 将请假申请的课程关联改为可选，兼容学生提交不关联具体课程的请假申请。
-- 仅调整字段约束，不删除或修改既有请假记录。
ALTER TABLE leave_request
    MODIFY COLUMN course_id VARCHAR(20) NULL COMMENT '课程ID，可为空表示不关联具体课程';
