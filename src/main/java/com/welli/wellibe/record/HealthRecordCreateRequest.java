package com.welli.wellibe.record;

import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record HealthRecordCreateRequest(

        @NotNull
        HealthRecordType type,

        @NotNull
        Map<String, Object> value,

        String photoUrl
) {
}