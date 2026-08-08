package com.campus.education.service.llm.impl;

import com.campus.education.dto.agent.EducationMetricsOverviewDTO;
import com.campus.education.service.llm.LlmClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * DeepSeek OpenAI 兼容 Chat Completions 客户端。
 *
 * 不注入 Mapper 或业务 Service，因此不能访问数据库；请求提示词也明确禁止生成 SQL。
 */
@Service
public class DeepSeekLlmClient implements LlmClient {

    private static final String CHAT_COMPLETIONS_PATH = "/chat/completions";
    private static final List<String> SUPPORTED_INTENTS = Arrays.asList(
            "overview", "grade", "attendance", "course_load", "academic_risk", "grade_trend",
            "failed_course", "attendance_abnormal", "student_comparison");

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final String model;
    private final String apiKey;

    public DeepSeekLlmClient(RestTemplateBuilder restTemplateBuilder,
                             ObjectMapper objectMapper,
                             @Value("${deepseek.base-url:https://api.deepseek.com}") String baseUrl,
                             @Value("${deepseek.model:deepseek-v4-flash}") String model,
                             @Value("${deepseek.api-key:}") String apiKey) {
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(3))
                .setReadTimeout(Duration.ofSeconds(8))
                .build();
        this.objectMapper = objectMapper;
        this.baseUrl = baseUrl;
        this.model = model;
        this.apiKey = apiKey;
    }

    @Override
    public String recognizeIntent(String question) {
        if (!isAvailable()) {
            return null;
        }

        String systemPrompt = "你是校园教务数据分析 Agent 的工具选择器。"
                + "只能从 overview、grade、attendance、course_load、academic_risk、grade_trend、"
                + "failed_course、attendance_abnormal、student_comparison 中选择一个工具。"
                + "禁止生成 SQL，禁止访问数据库，禁止调用任何未列出的工具。"
                + "只返回 JSON，例如：{\"intent\":\"grade\"}。";
        String content = callChat(systemPrompt, question);
        if (!StringUtils.hasText(content)) {
            return null;
        }

        try {
            JsonNode root = objectMapper.readTree(removeCodeFence(content));
            String intent = root.path("intent").asText(null);
            return isSupportedIntent(intent) ? intent : null;
        } catch (Exception exception) {
            return null;
        }
    }

    @Override
    public String explain(String question, String intent, Map<String, Object> metrics,
                          EducationMetricsOverviewDTO overview) {
        if (!isAvailable()) {
            return null;
        }

        try {
            Map<String, Object> context = new LinkedHashMap<>();
            context.put("question", question);
            context.put("intent", intent);
            context.put("metrics", metrics);
            context.put("overview", overview);
            String systemPrompt = "你是校园教务数据分析 Agent 的结果解读器。"
                    + "只能基于用户提供的已查询指标解释结果，不得编造数据。"
                    + "禁止生成 SQL，禁止访问数据库，回答不超过 120 个中文字符。";
            String answer = callChat(systemPrompt, objectMapper.writeValueAsString(context));
            return isSafeExplanation(answer) ? answer.trim() : null;
        } catch (Exception exception) {
            return null;
        }
    }

    private String callChat(String systemPrompt, String userPrompt) {
        try {
            Map<String, Object> request = new LinkedHashMap<>();
            request.put("model", model);
            request.put("temperature", 0);
            request.put("messages", Arrays.asList(
                    message("system", systemPrompt), message("user", userPrompt)));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey.trim());
            ResponseEntity<String> response = restTemplate.postForEntity(
                    baseUrl + CHAT_COMPLETIONS_PATH,
                    new HttpEntity<>(request, headers),
                    String.class);
            JsonNode content = objectMapper.readTree(response.getBody())
                    .path("choices").path(0).path("message").path("content");
            return content.isTextual() ? content.asText() : null;
        } catch (RestClientException exception) {
            return null;
        } catch (Exception exception) {
            return null;
        }
    }

    private Map<String, String> message(String role, String content) {
        Map<String, String> message = new LinkedHashMap<>();
        message.put("role", role);
        message.put("content", content);
        return message;
    }

    private boolean isAvailable() {
        return StringUtils.hasText(apiKey);
    }

    private boolean isSupportedIntent(String intent) {
        return SUPPORTED_INTENTS.contains(intent);
    }

    private boolean isSafeExplanation(String answer) {
        if (!StringUtils.hasText(answer) || answer.trim().length() > 500) {
            return false;
        }
        String normalized = answer.toLowerCase(Locale.ROOT);
        return !(normalized.contains("```sql")
                || normalized.contains("select ")
                || normalized.contains("insert ")
                || normalized.contains("update ")
                || normalized.contains("delete ")
                || normalized.contains("drop ")
                || normalized.contains("alter ")
                || normalized.contains("create table"));
    }

    private String removeCodeFence(String content) {
        String normalized = content.trim();
        if (!normalized.startsWith("```")) {
            return normalized;
        }
        int firstNewLine = normalized.indexOf('\n');
        int lastFence = normalized.lastIndexOf("```");
        return firstNewLine >= 0 && lastFence > firstNewLine
                ? normalized.substring(firstNewLine + 1, lastFence).trim()
                : normalized;
    }
}
