package com.campus.education.service.agent;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AcademicWarningAssessment {

    private String riskLevel;

    private Integer riskScore;

    private List<String> triggeredRules;

    private String riskReason;

    private String interventionSuggestion;
}
