package com.welli.wellibe.analysis;

/**
 * Parsed result of an OpenAI analysis call (or the rule-based fallback).
 */
public record AiAnalysisResult(
        int conditionDelta,
        String summary,
        String feedbackText
) {
}
