package com.campus.education.dto.agent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 学业预警列表返回对象，用于展示需要重点关注的学生及其主要风险信息。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AcademicWarningListDTO {

    /** 学号，也是学生主键。 */
    private String studentId;

    /** 学生姓名。 */
    private String studentName;

    /** 院系主键。 */
    private String departmentId;

    /** 专业主键。 */
    private String majorId;

    /** 专业名称，供预警列表直接展示。 */
    private String majorName;

    /** 班级主键。 */
    private String classId;

    /** 班级名称，供预警列表直接展示。 */
    private String className;

    /** 风险等级，约定取值为 high、medium、low。 */
    private String riskLevel;

    /** 规则计算得到的风险分数。 */
    private Integer riskScore;

    /** 当前最高优先级风险的简要说明。 */
    private String riskReason;

    /** 本次分析命中的预警规则，供列表直接展示和核对。 */
    private List<String> triggeredRules;

    /** 本次分析命中的预警规则数量，供轻量统计展示。 */
    private Integer triggeredRuleCount;

    public String getStudentNo() {
        return studentId;
    }

    public String getLevel() {
        return riskLevel;
    }

    public Integer getScore() {
        return riskScore;
    }

    public String getReason() {
        return riskReason;
    }
}
