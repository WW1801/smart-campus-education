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
    private String studentNo;
    @TableField(exist = false)
    private String studentName;
    private String courseId;
    @TableField(exist = false)
    private String courseName;
    private String semesterId;
    private LocalDate date;
    private String status;
    /** manual-人工录入；leave_request-请假审批同步。 */
    private String recordSource;
    private String sourceRequestId;
    private String approvedBy;
    private LocalDateTime syncedAt;
    /** 人工锁定后，任何自动同步和普通编辑都不得覆盖。 */
    private Boolean manualLocked;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
