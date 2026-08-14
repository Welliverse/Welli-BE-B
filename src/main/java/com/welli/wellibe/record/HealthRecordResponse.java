package com.welli.wellibe.record;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@AllArgsConstructor
public class HealthRecordResponse {

    private Long recordId;
    private HealthRecordType type;
    private Map<String, Object> value;
    private String photoUrl;
    private LocalDateTime recordedAt;
}