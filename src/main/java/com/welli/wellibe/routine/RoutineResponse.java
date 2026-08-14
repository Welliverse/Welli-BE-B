package com.welli.wellibe.routine;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@AllArgsConstructor
public class RoutineResponse {

    private Long routineId;
    private RoutineType routineType;
    private Integer priority;
    private LocalDate recommendedAt;
    private boolean isCompleted;
}