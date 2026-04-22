package com.campus.education.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class GraduationAuditVO {
    private String auditId;
    private String studentId;
    private String studentName;
    private String majorId;
    private String classId;
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
    private LocalDateTime updatedAt;
}
