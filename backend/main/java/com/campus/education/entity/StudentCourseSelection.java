package com.campus.education.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("student_course_selection")
public class StudentCourseSelection {
    @TableId(type = IdType.ASSIGN_ID)
    private String selectionId;
    private String studentId;
    private String courseId;
    private String scheduleId;
    private String semesterId;
    private String status;
    private LocalDateTime selectionTime;
    private LocalDateTime dropTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
