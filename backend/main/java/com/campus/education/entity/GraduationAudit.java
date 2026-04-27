package com.campus.education.entity;

/**
 * 毕业审核实体类，负责映射毕业审核相关业务数据。
 */

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("graduation_audit")
public class GraduationAudit {
    @TableId(type = IdType.ASSIGN_ID)
    private String auditId;
    private String studentId;
    private Double totalCredits;
    private Double requiredCredits;
    private Boolean compulsoryPass;
    private Double electivePublicCredits;
    private Double electiveMajorCredits;
    private Double gpa;
    private String status;
    private String auditOpinion;
    private Boolean degreeGranted;
    private LocalDateTime degreeAwardTime;
    private String certificateNo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
