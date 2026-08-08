package com.campus.education.dto.agent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 学业预警详情返回对象，用于说明单个学生的风险证据、命中规则和干预建议。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AcademicWarningDetailDTO {

    /** 学号，也是学生主键。 */
    private String studentId;

    /** 学生姓名。 */
    private String studentName;

    /** 专业名称。 */
    private String majorName;

    /** 班级名称。 */
    private String className;

    /** 分析所属学期主键；为空时表示跨学期汇总。 */
    private String semesterId;

    /** 综合风险等级，约定取值为 high、medium、low。 */
    private String riskLevel;

    /** 规则计算得到的综合风险分数。 */
    private Integer riskScore;

    /** 已审核不及格课程数量。 */
    private Integer failedCourseCount;

    /** 缺勤考勤记录数量。 */
    private Integer absentCount;

    /** 迟到考勤记录数量。 */
    private Integer lateCount;

    /** 毕业审核状态，例如 approved、rejected、pending。 */
    private String graduationAuditStatus;

    /** 命中的预警规则说明列表。 */
    private List<String> triggeredRules;

    /** 当前命中规则汇总形成的风险原因。 */
    private String riskReason;

    /** 面向教师或教务人员的干预建议。 */
    private String interventionSuggestion;

    public String getStudentNo() {
        return studentId;
    }

    public String getLevel() {
        return riskLevel;
    }

    public Integer getScore() {
        return riskScore;
    }

    public List<String> getRules() {
        return triggeredRules;
    }

    public String getReason() {
        return riskReason;
    }

    public String getSuggestion() {
        return interventionSuggestion;
    }
}
