package com.welli.wellibe.analysis;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class AnalysisResponse {

    private Long analysisId;
    private String summary;
    private String feedbackText;
    private Integer conditionDelta;
    private Integer conditionScore;
    private Integer growthScoreDelta;
    private Integer growthScore;
    private Integer growthStage;
    private String appearanceState;
    private LocalDateTime analyzedAt;
}
