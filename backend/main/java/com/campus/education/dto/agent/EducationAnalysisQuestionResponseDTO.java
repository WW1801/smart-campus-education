package com.campus.education.dto.agent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 教务分析问答响应对象，用于返回识别结果、可解释答案和结构化明细数据。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EducationAnalysisQuestionResponseDTO {

    /** 规则识别出的问数意图编码，例如 grade_pass_rate。 */
    private String intentCode;

    /** 规则识别出的问数意图名称。 */
    private String intentName;

    /** 可直接展示给用户的问答结论。 */
    private String answer;

    /** 意图识别来源：deepseek 或 keyword。 */
    private String intentSource;

    /** 结果解释来源：deepseek 或 keyword。 */
    private String answerSource;

    /** 生成结论所依据的核心指标概览。 */
    private EducationMetricsOverviewDTO metricsOverview;

    /** 规则生成的补充分析洞察。 */
    private List<String> insights;

    /** 结构化明细行；每个 Map 的字段由具体问数意图确定。 */
    private List<Map<String, Object>> detailRows;
}
