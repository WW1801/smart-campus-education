package com.campus.education.entity;

/**
 * 课表实体类，负责映射课表相关业务数据。
 */

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("course_schedule")
public class Schedule {
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
}
