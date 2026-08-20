package com.welli.wellibe.analysis;

import com.welli.wellibe.user.HealthGoal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class OpenAiFeedbackService {

    private final RestClient restClient = RestClient.create();

    @Value("${openai.api-key:}")
    private String apiKey;

    @Value("${openai.model:gpt-5.2}")
    private String model;

    public String createFeedback(
            HealthGoal healthGoal,
            String recordSummary,
            int conditionScore,
            int conditionDelta,
            String fallbackFeedback
    ) {
        if (apiKey == null || apiKey.isBlank()) {
            return fallbackFeedback;
        }

        try {
            Map<String, Object> requestBody = Map.of(
                    "model", model,
                    "store", false,
                    "instructions", """
                            당신은 Welli 건강 습관 서비스의 따뜻한 코치입니다.
                            의료 진단이나 치료를 하지 마세요.
                            제공된 기록만 근거로 한국어 피드백을 2문장 이내로 작성하세요.
                            사용자를 비난하지 말고 오늘 실천할 수 있는 작은 행동을 제안하세요.
                            """,
                    "input", createPrompt(
                            healthGoal,
                            recordSummary,
                            conditionScore,
                            conditionDelta
                    ),
                    "text", Map.of("verbosity", "low")
            );

            Map<?, ?> response = restClient.post()
                    .uri("https://api.openai.com/v1/responses")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(Map.class);

            String feedback = extractOutputText(response);

            return feedback == null || feedback.isBlank()
                    ? fallbackFeedback
                    : feedback;
        } catch (Exception exception) {
            log.warn("OpenAI 피드백 생성에 실패해 규칙 기반 피드백을 사용합니다: {}", exception.getMessage());
            return fallbackFeedback;
        }
    }

    private String createPrompt(
            HealthGoal healthGoal,
            String recordSummary,
            int conditionScore,
            int conditionDelta
    ) {
        return """
                건강 목표: %s
                최근 건강 기록: %s
                현재 컨디션 점수: %d점
                이번 분석 변화량: %d점
                """.formatted(
                healthGoal == null ? "미설정" : healthGoal.name(),
                recordSummary,
                conditionScore,
                conditionDelta
        );
    }

    private String extractOutputText(Map<?, ?> response) {
        if (response == null || !(response.get("output") instanceof List<?> outputItems)) {
            return null;
        }

        for (Object outputItem : outputItems) {
            if (!(outputItem instanceof Map<?, ?> item)
                    || !(item.get("content") instanceof List<?> contentItems)) {
                continue;
            }

            for (Object contentItem : contentItems) {
                if (contentItem instanceof Map<?, ?> content
                        && "output_text".equals(content.get("type"))
                        && content.get("text") instanceof String text) {
                    return text.trim();
                }
            }
        }

        return null;
    }
}
