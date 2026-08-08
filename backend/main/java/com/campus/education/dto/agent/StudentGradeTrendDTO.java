package com.campus.education.dto.agent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 学业预警使用的学生成绩趋势：所有字段均由固定成绩查询和规则计算产生。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentGradeTrendDTO {

    /** 已核验的学生标识和姓名；姓名缺失时不输出个人趋势结论。 */
    private String studentId;
    private String studentName;

    /** 每个学期的平均分、不及格课程数和课程数。 */
    private List<SemesterGradeTrendDTO> semesters;

    /** 上升、下降、基本稳定或数据不足。 */
    private String trendDirection;

    /** 基于最近两个有成绩学期得出的趋势结论。 */
    private String trendConclusion;

    /** 基于不及格数和平均分变化得出的预警风险变化。 */
    private String riskChange;

    /** 无法确认学生身份或没有足够数据时的说明。 */
    private String dataNotice;
}
