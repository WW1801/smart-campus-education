package com.campus.education.service.llm;

import com.campus.education.dto.agent.EducationMetricsOverviewDTO;

import java.util.Map;

/**
 * 受限的大模型能力边界：只识别固定工具并解读后端已经查询出的指标。
 */
public interface LlmClient {

    /**
     * 只能返回 overview、grade、attendance 或 course_load；无法识别时返回 null。
     */
    String recognizeIntent(String question);

    /**
     * 基于固定 Service 返回的指标生成说明；不可用时返回 null。
     */
    String explain(String question, String intent, Map<String, Object> metrics,
                   EducationMetricsOverviewDTO overview);
}
