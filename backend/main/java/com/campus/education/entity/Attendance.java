package com.campus.education.entity;

/**
 * 考勤实体类，负责映射考勤相关业务数据。
 */

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("attendance")
public class Attendance {
    @TableId(type = IdType.ASSIGN_ID)
    private String attendanceId;
    private String studentId;
    @TableField(exist = false)
    private String studentName;
    private String courseId;
    @TableField(exist = false)
    private String courseName;
    private String semesterId;
    private LocalDate date;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
