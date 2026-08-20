package com.welli.wellibe.analysis;

import com.welli.wellibe.character.Character;
import com.welli.wellibe.character.CharacterRepository;
import com.welli.wellibe.record.HealthRecord;
import com.welli.wellibe.record.HealthRecordRepository;
import com.welli.wellibe.record.HealthRecordType;
import com.welli.wellibe.user.User;
import com.welli.wellibe.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisService {

    private final UserRepository userRepository;
    private final CharacterRepository characterRepository;
    private final HealthRecordRepository healthRecordRepository;
    private final AnalysisResultRepository analysisResultRepository;
    private final AiAnalysisClient aiAnalysisClient;

    @Transactional
    public AnalysisResponse run(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new IllegalArgumentException("존재하지 않는 사용자입니다.")
                );

        Character character = characterRepository.findByUserId(user.getId())
                .orElseThrow(() ->
                        new IllegalArgumentException("캐릭터가 존재하지 않습니다.")
                );

        int beforeScore = character.getConditionScore();

        AiAnalysisResult aiResult = analyzeWithFallback(user.getId(), beforeScore);

        character.updateCondition(beforeScore + aiResult.conditionDelta());

        AnalysisResult result = AnalysisResult.builder()
                .user(user)
                .summary(aiResult.summary())
                .feedbackText(aiResult.feedbackText())
                .conditionDelta(character.getConditionScore() - beforeScore)
                .build();

        AnalysisResult savedResult = analysisResultRepository.save(result);

        return new AnalysisResponse(
                savedResult.getId(),
                savedResult.getSummary(),
                savedResult.getFeedbackText(),
                savedResult.getConditionDelta(),
                character.getConditionScore(),
                character.getAppearanceState(),
                savedResult.getAnalyzedAt()
        );
    }

    @Transactional(readOnly = true)
    public AnalysisResponse getLatest(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new IllegalArgumentException("존재하지 않는 사용자입니다.")
                );

        Character character = characterRepository.findByUserId(user.getId())
                .orElseThrow(() ->
                        new IllegalArgumentException("캐릭터가 존재하지 않습니다.")
                );

        AnalysisResult result = analysisResultRepository
                .findTopByUserIdOrderByAnalyzedAtDesc(user.getId())
                .orElseThrow(() ->
                        new IllegalArgumentException("분석 결과가 존재하지 않습니다.")
                );

        return new AnalysisResponse(
                result.getId(),
                result.getSummary(),
                result.getFeedbackText(),
                result.getConditionDelta(),
                character.getConditionScore(),
                character.getAppearanceState(),
                result.getAnalyzedAt()
        );
    }

    /**
     * Primary path: ask OpenAI to judge the condition delta + write the copy.
     * If that call fails for any reason (no API key, network error, rate
     * limit, bad response), fall back to the old rule-based logic so
     * /analysis/run never breaks.
     */
    private AiAnalysisResult analyzeWithFallback(Long userId, int currentConditionScore) {
        AiAnalysisRequest request = buildAiRequest(userId, currentConditionScore);

        try {
            return aiAnalysisClient.analyze(request);
        } catch (Exception e) {
            log.warn("OpenAI 분석 실패, 규칙 기반 분석으로 대체합니다. userId={}", userId, e);
            return fallbackAnalyze(userId);
        }
    }

    private AiAnalysisRequest buildAiRequest(Long userId, int currentConditionScore) {
        return new AiAnalysisRequest(
                currentConditionScore,
                latestValue(userId, HealthRecordType.SLEEP),
                latestValue(userId, HealthRecordType.WATER),
                latestValue(userId, HealthRecordType.STRESS_EMOTION),
                latestValue(userId, HealthRecordType.EXERCISE),
                latestValue(userId, HealthRecordType.MEAL)
        );
    }

    private Map<String, Object> latestValue(Long userId, HealthRecordType type) {
        return healthRecordRepository
                .findTopByUserIdAndTypeOrderByRecordedAtDesc(userId, type)
                .map(HealthRecord::getValue)
                .orElse(null);
    }

    // ---- Rule-based fallback (used only if the OpenAI call fails) ----

    private AiAnalysisResult fallbackAnalyze(Long userId) {
        int delta = calculateDelta(userId);
        String summary = createSummary(delta);
        String feedbackText = createFeedback(delta);
        return new AiAnalysisResult(delta, summary, feedbackText);
    }

    private int calculateDelta(Long userId) {
        int delta = 0;

        HealthRecord sleep = healthRecordRepository
                .findTopByUserIdAndTypeOrderByRecordedAtDesc(
                        userId,
                        HealthRecordType.SLEEP
                )
                .orElse(null);

        if (sleep != null && sleep.getValue().get("hours") instanceof Number hours) {
            if (hours.doubleValue() < 6) {
                delta -= 10;
            } else if (hours.doubleValue() < 7) {
                delta -= 3;
            } else {
                delta += 5;
            }
        }

        HealthRecord water = healthRecordRepository
                .findTopByUserIdAndTypeOrderByRecordedAtDesc(
                        userId,
                        HealthRecordType.WATER
                )
                .orElse(null);

        if (water != null && water.getValue().get("ml") instanceof Number ml) {
            if (ml.doubleValue() < 1000) {
                delta -= 5;
            } else if (ml.doubleValue() >= 1500) {
                delta += 3;
            }
        }

        HealthRecord stress = healthRecordRepository
                .findTopByUserIdAndTypeOrderByRecordedAtDesc(
                        userId,
                        HealthRecordType.STRESS_EMOTION
                )
                .orElse(null);

        if (stress != null && stress.getValue().get("level") instanceof Number level) {
            if (level.doubleValue() >= 4) {
                delta -= 8;
            } else if (level.doubleValue() <= 2) {
                delta += 2;
            }
        }

        return delta;
    }

    private String createSummary(int delta) {
        if (delta > 0) {
            return "최근 건강 기록이 캐릭터 컨디션에 긍정적으로 반영되었습니다.";
        }

        if (delta < 0) {
            return "최근 건강 기록에서 관리가 필요한 항목이 확인되었습니다.";
        }

        return "분석할 수 있는 건강 기록이 충분하지 않거나 컨디션 변화가 없습니다.";
    }

    private String createFeedback(int delta) {
        if (delta > 0) {
            return "오늘의 웰리는 컨디션이 좋아지고 있어요. 이 리듬을 유지해 보세요!";
        }

        if (delta < 0) {
            return "오늘의 웰리가 조금 지쳐 있어요. 수면과 수분 섭취를 챙겨 주세요.";
        }

        return "오늘의 웰리는 평온한 상태예요. 작은 건강 습관을 이어가 보세요.";
    }
}
