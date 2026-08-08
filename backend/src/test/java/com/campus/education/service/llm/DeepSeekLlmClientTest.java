package com.campus.education.service.llm;

import com.campus.education.dto.agent.EducationMetricsOverviewDTO;
import com.campus.education.service.llm.impl.DeepSeekLlmClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertNull;

class DeepSeekLlmClientTest {

    @Test
    void shouldReturnNullWithoutApiKeyAndAvoidExternalCall() {
        DeepSeekLlmClient client = new DeepSeekLlmClient(
                new RestTemplateBuilder(),
                new ObjectMapper(),
                "https://api.deepseek.com",
                "deepseek-v4-flash",
                "");

        assertNull(client.recognizeIntent("请分析成绩"));
        assertNull(client.explain(
                "请分析成绩",
                "grade",
                Collections.<String, Object>emptyMap(),
                EducationMetricsOverviewDTO.builder().build()));
    }
}
