package com.campus.education.service.agent;

import java.util.function.Function;
import java.util.function.Predicate;

public class AcademicWarningRule {

    private final String code;
    private final String level;
    private final int score;
    private final Predicate<AcademicWarningContext> condition;
    private final Function<AcademicWarningContext, String> reasonBuilder;
    private final String suggestion;

    public AcademicWarningRule(String code, String level, int score,
                               Predicate<AcademicWarningContext> condition,
                               Function<AcademicWarningContext, String> reasonBuilder,
                               String suggestion) {
        this.code = code;
        this.level = level;
        this.score = score;
        this.condition = condition;
        this.reasonBuilder = reasonBuilder;
        this.suggestion = suggestion;
    }

    public RuleEvaluation evaluate(AcademicWarningContext context) {
        if (!condition.test(context)) {
            return null;
        }
        return new RuleEvaluation(code, level, score, reasonBuilder.apply(context), suggestion);
    }
}
