package com.campus.education.service.agent;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AcademicWarningRuleEngineTest {

    private final AcademicWarningRuleEngine engine = new AcademicWarningRuleEngine();

    @Test
    void shouldReturnLowRiskBelowAllThresholds() {
        AcademicWarningAssessment assessment = engine.assess(context(0, 2, "approved"));

        assertEquals("low", assessment.getRiskLevel());
        assertEquals(0, assessment.getRiskScore());
        assertTrue(assessment.getTriggeredRules().isEmpty());
    }

    @Test
    void shouldReturnMediumAtSingleFailedCourseBoundary() {
        AcademicWarningAssessment assessment = engine.assess(context(1, 0, "approved"));

        assertEquals("medium", assessment.getRiskLevel());
        assertEquals(25, assessment.getRiskScore());
        assertEquals(1, assessment.getTriggeredRules().size());
        assertTrue(assessment.getRiskReason().contains("不及格课程数为 1 门"));
    }

    @Test
    void shouldReturnHighAtTwoFailedCoursesBoundary() {
        AcademicWarningAssessment assessment = engine.assess(context(2, 0, "approved"));

        assertEquals("high", assessment.getRiskLevel());
        assertEquals(40, assessment.getRiskScore());
        assertEquals(1, assessment.getTriggeredRules().size());
        assertTrue(assessment.getRiskReason().contains("不及格课程数达到 2 门"));
    }

    @Test
    void shouldReturnMediumAtThreeAbsencesBoundary() {
        AcademicWarningAssessment assessment = engine.assess(context(0, 3, "approved"));

        assertEquals("medium", assessment.getRiskLevel());
        assertEquals(30, assessment.getRiskScore());
        assertEquals(1, assessment.getTriggeredRules().size());
        assertTrue(assessment.getRiskReason().contains("缺勤次数达到 3 次"));
    }

    @Test
    void shouldReturnHighWhenGraduationAuditRejected() {
        AcademicWarningAssessment assessment = engine.assess(context(0, 0, "rejected"));

        assertEquals("high", assessment.getRiskLevel());
        assertEquals(40, assessment.getRiskScore());
        assertEquals(1, assessment.getTriggeredRules().size());
        assertTrue(assessment.getRiskReason().contains("毕业审核未通过"));
    }

    @Test
    void shouldUseHighestRiskAndCapScoreWhenMultipleRulesHit() {
        AcademicWarningAssessment assessment = engine.assess(context(2, 3, "rejected"));

        assertEquals("high", assessment.getRiskLevel());
        assertEquals(100, assessment.getRiskScore());
        assertEquals(4, assessment.getTriggeredRules().size());
        assertTrue(assessment.getRiskReason().contains("成绩、考勤、毕业审核风险同时存在"));
    }

    private AcademicWarningContext context(int failedCourseCount, int absentCount, String auditStatus) {
        return AcademicWarningContext.builder()
                .failedCourseCount(failedCourseCount)
                .absentCount(absentCount)
                .lateCount(0)
                .graduationAuditStatus(auditStatus)
                .build();
    }
}
