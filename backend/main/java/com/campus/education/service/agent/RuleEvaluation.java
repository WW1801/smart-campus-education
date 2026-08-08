package com.campus.education.service.agent;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RuleEvaluation {

    private String code;

    private String level;

    private int score;

    private String reason;

    private String suggestion;
}
