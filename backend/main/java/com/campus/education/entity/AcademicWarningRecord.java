package com.campus.education.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 学业预警历史记录，保存 Agent 每次计算后的风险结果和后续处理闭环。
 */
@Data
@TableName("academic_warning_record")
public class AcademicWarningRecord {
    @TableId(type = IdType.ASSIGN_ID)
    private String recordId;
    private String studentId;
    private String semesterId;
    private String riskLevel;
    private Integer riskScore;
    private String riskReason;
    private String triggeredRules;
    private LocalDateTime calculatedAt;
    private String processStatus;
    private String processOpinion;
    private String processedBy;
    private LocalDateTime processedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
