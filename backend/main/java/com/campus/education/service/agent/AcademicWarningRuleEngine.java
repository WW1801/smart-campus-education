package com.campus.education.service.agent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class AcademicWarningRuleEngine {

    private static final List<AcademicWarningRule> RULES = Collections.unmodifiableList(Arrays.asList(
            new AcademicWarningRule(
                    "GRADE_FAILED_MULTI",
                    "high",
                    40,
                    context -> context.getFailedCourseCount() >= 2,
                    context -> "不及格课程数达到 " + context.getFailedCourseCount() + " 门（>= 2）",
                    "安排辅导员和任课教师联合约谈，制定补考、重修及阶段学习计划"),
            new AcademicWarningRule(
                    "GRADE_FAILED_SINGLE",
                    "medium",
                    25,
                    context -> context.getFailedCourseCount() == 1,
                    context -> "不及格课程数为 1 门",
                    "提醒学生准备补考或重修，并跟踪该课程后续学习情况"),
            new AcademicWarningRule(
                    "ATTENDANCE_ABSENT_3",
                    "medium",
                    30,
                    context -> context.getAbsentCount() >= 3,
                    context -> "缺勤次数达到 " + context.getAbsentCount() + " 次（>= 3）",
                    "开展考勤约谈，联系任课教师持续跟踪课堂出勤"),
            new AcademicWarningRule(
                    "GRADUATION_REJECTED",
                    "high",
                    40,
                    AcademicWarningContext::isGraduationRejected,
                    context -> "毕业审核未通过，存在毕业风险",
                    "优先核对毕业审核未通过项，制定补修或材料整改计划"),
            new AcademicWarningRule(
                    "COMBINED_GRADE_ATTENDANCE_GRADUATION",
                    "high",
                    20,
                    context -> context.hasFailedCourses()
                            && context.hasAttendanceRisk()
                            && context.isGraduationRejected(),
                    context -> "成绩、考勤、毕业审核风险同时存在",
                    "建立专项干预台账，由管理员统筹成绩补救、出勤改进和毕业审核整改进度")
    ));

    public AcademicWarningAssessment assess(AcademicWarningContext context) {
        List<RuleEvaluation> hits = new ArrayList<>();
        Set<String> suggestions = new LinkedHashSet<>();
        int riskScore = 0;
        String riskLevel = "low";

        for (AcademicWarningRule rule : RULES) {
            RuleEvaluation hit = rule.evaluate(context);
            if (hit == null) {
                continue;
            }
            hits.add(hit);
            suggestions.add(hit.getSuggestion());
            riskScore += hit.getScore();
            // 多条规则同时命中时，按 high > medium > low 保留最高风险等级。
            if (riskLevelWeight(hit.getLevel()) > riskLevelWeight(riskLevel)) {
                riskLevel = hit.getLevel();
            }
        }

        List<String> triggeredRules = new ArrayList<>();
        for (RuleEvaluation hit : hits) {
            triggeredRules.add(hit.getReason());
        }

        String riskReason = triggeredRules.isEmpty()
                ? "当前未命中学业预警规则"
                : String.join("；", triggeredRules);
        String interventionSuggestion = suggestions.isEmpty()
                ? "当前无需专项干预，继续按常规周期跟踪成绩、考勤和毕业审核状态"
                : String.join("；", suggestions);

        return AcademicWarningAssessment.builder()
                .riskLevel(riskLevel)
                // 风险分累加后封顶，避免组合规则导致展示分超过 100。
                .riskScore(Math.min(riskScore, 100))
                .triggeredRules(triggeredRules)
                .riskReason(riskReason)
                .interventionSuggestion(interventionSuggestion)
                .build();
    }

    public int riskLevelWeight(String level) {
        if ("high".equals(level)) {
            return 3;
        }
        if ("medium".equals(level)) {
            return 2;
        }
        return 1;
    }
}
