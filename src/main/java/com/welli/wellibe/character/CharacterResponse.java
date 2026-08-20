package com.welli.wellibe.character;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CharacterResponse {

    private Long characterId;
    private Integer growthStage;
    private Integer growthScore;
    private Integer conditionScore;
    private String appearanceState;
}
