package com.welli.wellibe.user;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ProfileUpdateRequest(

        @NotNull
        @Min(1)
        @Max(120)
        Integer age,

        @NotBlank
        String gender,

        @NotBlank
        String healthGoal
) {
}