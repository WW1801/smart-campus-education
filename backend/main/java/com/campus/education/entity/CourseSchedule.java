package com.campus.education.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("course_schedule")
public class CourseSchedule {
    @TableId(type = IdType.ASSIGN_ID)
    private String scheduleId;
    private String mode;
    private Integer maxStudents;
    private Integer currentStudents;
    private String courseId;
    private String teacherId;
    private String classId;
    private String semesterId;
    private String classroomId;
    private Integer dayOfWeek;
    private Integer startPeriod;
    private Integer endPeriod;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
