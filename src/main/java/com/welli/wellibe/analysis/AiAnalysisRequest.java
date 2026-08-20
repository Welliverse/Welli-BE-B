package com.welli.wellibe.analysis;

import java.util.Map;

/**
 * Snapshot of a user's latest health records + current condition score,
 * sent to OpenAI so it can judge how the character's condition should change.
 * Values are the raw {@link com.welli.wellibe.record.HealthRecord#getValue()}
 * JSON maps (e.g. sleep -> {"hours": 6.5}); null means no record of that type yet.
 */
public record AiAnalysisRequest(
        int currentConditionScore,
        Map<String, Object> sleep,
        Map<String, Object> water,
        Map<String, Object> stressEmotion,
        Map<String, Object> exercise,
        Map<String, Object> meal
) {
}
