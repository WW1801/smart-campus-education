package com.campus.education.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 请假申请实体，保存学生申请、审批结果和考勤同步状态。 */
@Data
@TableName("leave_request")
public class LeaveRequest {
    @TableId(type = IdType.ASSIGN_ID)
    private String requestId;
    private String studentId;
    private String courseId;
    private String semesterId;
    private LocalDate startDate;
    private LocalDate endDate;
    private String leaveType;
    private String reason;
    private String status;
    private LocalDateTime submittedAt;
    private String processedBy;
    private LocalDateTime processedAt;
    private String processOpinion;
    private String attendanceSyncStatus;
    private String syncFailureReason;
    @JsonIgnore
    private String requestHash;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableField(exist = false)
    private String studentNo;
    @TableField(exist = false)
    private String studentName;
    @TableField(exist = false)
    private String courseName;
    @TableField(exist = false)
    private String semesterName;
    @TableField(exist = false)
    private String processedByName;
}
