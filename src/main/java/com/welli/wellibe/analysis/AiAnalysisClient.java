package com.welli.wellibe.analysis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Calls OpenAI's Chat Completions API to judge how a user's recent health
 * records should move their character's condition score, and to write the
 * summary/feedback copy shown in the app. Uses Structured Outputs
 * (response_format=json_schema) so the response is guaranteed valid JSON
 * matching {@link AiAnalysisResult}'s shape.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiAnalysisClient {

    // Created directly rather than injected: this project's Spring Boot setup
    // doesn't auto-register a Jackson ObjectMapper bean, and this is the only
    // place we need one (parsing the JSON string OpenAI returns).
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final String SYSTEM_PROMPT = """
            당신은 웰니스 앱 'Welli'의 건강 데이터 분석기입니다.
            사용자의 최근 수면/수분/스트레스/운동/식사 기록과 현재 캐릭터 컨디션
            점수(0~100)를 보고 다음 세 가지를 한국어로 생성하세요.
            1. conditionDelta: 컨디션 점수 변화량. -20 ~ +20 사이의 정수.
               기록이 좋으면 양수, 나쁘면 음수, 기록이 거의 없으면 0에 가깝게.
            2. summary: 이번 분석 결과를 한두 문장으로 요약 (사용자에게 보여줄 문장).
            3. feedbackText: 캐릭터가 사용자에게 말하듯 다정하게 건네는 조언/격려 메시지.
            데이터가 부족한 항목은 무리하게 추측하지 말고 있는 정보만 근거로 판단하세요.
            """;

    @SuppressWarnings("unchecked")
    private static final Map<String, Object> RESPONSE_FORMAT = Map.of(
            "type", "json_schema",
            "json_schema", Map.of(
                    "name", "analysis_result",
                    "strict", true,
                    "schema", Map.of(
                            "type", "object",
                            "properties", Map.of(
                                    "conditionDelta", Map.of(
                                            "type", "integer",
                                            "description", "컨디션 점수 변화량 (-20 ~ +20 정수)"
                                    ),
                                    "summary", Map.of(
                                            "type", "string",
                                            "description", "분석 결과 한두 문장 요약 (한국어)"
                                    ),
                                    "feedbackText", Map.of(
                                            "type", "string",
                                            "description", "캐릭터 말투의 조언/격려 메시지 (한국어)"
                                    )
                            ),
                            "required", List.of("conditionDelta", "summary", "feedbackText"),
                            "additionalProperties", false
                    )
            )
    );

    private final RestClient openAiRestClient;

    @Value("${openai.model}")
    private String model;

    public AiAnalysisResult analyze(AiAnalysisRequest request) {
        try {
            Map<String, Object> requestBody = Map.of(
                    "model", model,
                    "temperature", 0.4,
                    "messages", List.of(
                            Map.of("role", "system", "content", SYSTEM_PROMPT),
                            Map.of("role", "user", "content", buildUserPrompt(request))
                    ),
                    "response_format", RESPONSE_FORMAT
            );

            String responseBody = openAiRestClient.post()
                    .uri("/chat/completions")
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            JsonNode response = OBJECT_MAPPER.readTree(responseBody);

            String content = response
                    .path("choices").path(0)
                    .path("message").path("content")
                    .asText();

            JsonNode parsed = OBJECT_MAPPER.readTree(content);

            return new AiAnalysisResult(
                    parsed.path("conditionDelta").asInt(0),
                    parsed.path("summary").asText(""),
                    parsed.path("feedbackText").asText("")
            );
        } catch (AiAnalysisException e) {
            throw e;
        } catch (Exception e) {
            throw new AiAnalysisException("OpenAI 분석 호출에 실패했습니다.", e);
        }
    }

    private String buildUserPrompt(AiAnalysisRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("현재 컨디션 점수: ").append(request.currentConditionScore()).append("\n");
        appendRecord(sb, "수면", request.sleep());
        appendRecord(sb, "수분 섭취", request.water());
        appendRecord(sb, "스트레스/감정", request.stressEmotion());
        appendRecord(sb, "운동", request.exercise());
        appendRecord(sb, "식사", request.meal());
        return sb.toString();
    }

    private void appendRecord(StringBuilder sb, String label, Map<String, Object> value) {
        if (value == null || value.isEmpty()) {
            sb.append("- ").append(label).append(": 기록 없음\n");
            return;
        }
        sb.append("- ").append(label).append(": ").append(value).append("\n");
    }
}
