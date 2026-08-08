package com.campus.education.dto.agent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 教务指标概览对象，用于统一承载数据分析 Agent 首页和问答结果的核心统计值。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EducationMetricsOverviewDTO {

    /** 纳入本次统计范围的学生总数。 */
    private Long studentCount;

    /** 学籍状态为在读的学生数量。 */
    private Long activeStudentCount;

    /** 纳入统计范围的成绩记录数量。 */
    private Long gradeRecordCount;

    /** 已审核且不及格的成绩记录数量。 */
    private Long failedCourseCount;

    /** 成绩通过率，取值范围为 0 到 100。 */
    private Double gradePassRate;

    /** 纳入统计范围的考勤记录数量。 */
    private Long attendanceRecordCount;

    /** 缺勤、迟到、请假等异常考勤记录数量。 */
    private Long abnormalAttendanceCount;

    /** 毕业审核记录数量。 */
    private Long graduationAuditCount;

    /** 毕业审核通过数量。 */
    private Long approvedGraduationCount;

    /** 毕业审核未通过数量。 */
    private Long rejectedGraduationCount;

    /** 毕业审核通过率，取值范围为 0 到 100。 */
    private Double graduationApprovalRate;
}
