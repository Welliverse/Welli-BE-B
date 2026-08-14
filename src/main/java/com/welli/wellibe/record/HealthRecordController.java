package com.welli.wellibe.record;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/records")
@RequiredArgsConstructor
public class HealthRecordController {

    private final HealthRecordService healthRecordService;

    @PostMapping
    public ResponseEntity<HealthRecordResponse> create(
            @AuthenticationPrincipal String email,
            @Valid @RequestBody HealthRecordCreateRequest request
    ) {
        return ResponseEntity.ok(
                healthRecordService.create(email, request)
        );
    }

    @GetMapping
    public ResponseEntity<List<HealthRecordResponse>> getMyRecords(
            @AuthenticationPrincipal String email
    ) {
        return ResponseEntity.ok(
                healthRecordService.getMyRecords(email)
        );
    }
}