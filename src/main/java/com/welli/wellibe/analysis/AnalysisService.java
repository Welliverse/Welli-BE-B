package com.welli.wellibe.analysis;

import com.welli.wellibe.character.Character;
import com.welli.wellibe.character.CharacterRepository;
import com.welli.wellibe.record.HealthRecord;
import com.welli.wellibe.record.HealthRecordRepository;
import com.welli.wellibe.record.HealthRecordType;
import com.welli.wellibe.user.User;
import com.welli.wellibe.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AnalysisService {

    private final UserRepository userRepository;
    private final CharacterRepository characterRepository;
    private final HealthRecordRepository healthRecordRepository;
    private final AnalysisResultRepository analysisResultRepository;
    private final OpenAiFeedbackService openAiFeedbackService;

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
        int delta = calculateDelta(user.getId());

        character.updateCondition(beforeScore + delta);

        String summary = createSummary(delta);
        String fallbackFeedback = createFeedback(character.getConditionScore());
        String feedbackText = openAiFeedbackService.createFeedback(
                user.getHealthGoal(),
                createRecordSummary(user.getId()),
                character.getConditionScore(),
                character.getConditionScore() - beforeScore,
                fallbackFeedback
        );

        AnalysisResult result = AnalysisResult.builder()
                .user(user)
                .summary(summary)
                .feedbackText(feedbackText)
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

    private String createRecordSummary(Long userId) {
        return "수면: " + getLatestValue(userId, HealthRecordType.SLEEP, "hours", "시간")
                + ", 물 섭취: " + getLatestValue(userId, HealthRecordType.WATER, "ml", "ml")
                + ", 스트레스: " + getLatestValue(userId, HealthRecordType.STRESS_EMOTION, "level", "단계");
    }

    private String getLatestValue(
            Long userId,
            HealthRecordType type,
            String key,
            String unit
    ) {
        HealthRecord record = healthRecordRepository
                .findTopByUserIdAndTypeOrderByRecordedAtDesc(userId, type)
                .orElse(null);

        if (record == null || record.getValue().get(key) == null) {
            return "기록 없음";
        }

        return record.getValue().get(key) + unit;
    }

    private String createFeedback(int conditionScore) {
        if (conditionScore >= 80) {
            return "오늘의 웰리는 매우 좋은 컨디션이에요. 이 리듬을 유지해 보세요!";
        }

        if (conditionScore >= 60) {
            return "오늘의 웰리는 안정적인 컨디션이에요. 작은 건강 습관을 이어가 보세요.";
        }

        if (conditionScore >= 40) {
            return "오늘의 웰리는 조금 지쳐 있어요. 수면과 수분 섭취를 챙겨 주세요.";
        }

        return "오늘의 웰리가 많이 지쳐 있어요. 충분한 휴식과 작은 실천부터 시작해 보세요.";
    }
}
