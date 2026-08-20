package com.welli.wellibe.analysis;

/**
 * Parsed result of an OpenAI analysis call (or the rule-based fallback).
 */
public record AiAnalysisResult(
        int conditionDelta,
        int growthScoreDelta,
        String summary,
        String feedbackText
) {
    public AiAnalysisResult {
        conditionDelta = Math.max(-20, Math.min(20, conditionDelta));
        growthScoreDelta = Math.max(-5, Math.min(10, growthScoreDelta));
    }
}
