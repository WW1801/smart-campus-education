package com.campus.education.dto.agent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 学业预警历史和处理记录返回对象。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AcademicWarningRecordDTO {
    /** 预警历史记录 ID。 */
    private String recordId;
    /** 学生内部 ID（student.student_id），用于历史接口路径参数。 */
    private String studentId;
    /** 学生姓名，仅供页面展示。 */
    private String studentName;
    /** 计算预警时使用的学期 ID，跨学期计算时可为空。 */
    private String semesterId;
    /** 风险等级：high、medium、low。 */
    private String riskLevel;
    /** 风险规则计算得到的分数。 */
    private Integer riskScore;
    /** 命中规则汇总出的风险原因。 */
    private String riskReason;
    /** 数据库存储的规则 JSON 转换后的规则编码数组。 */
    private List<String> triggeredRules;
    /** 风险计算完成时间。 */
    private LocalDateTime calculatedAt;
    private String processStatus;
    private String processOpinion;
    private String processedBy;
    private LocalDateTime processedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
