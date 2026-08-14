package com.welli.wellibe.analysis;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/analysis")
@RequiredArgsConstructor
public class AnalysisController {

    private final AnalysisService analysisService;

    @PostMapping("/run")
    public ResponseEntity<AnalysisResponse> run(
            @AuthenticationPrincipal String email
    ) {
        return ResponseEntity.ok(analysisService.run(email));
    }
    @GetMapping("/latest")
    public ResponseEntity<AnalysisResponse> getLatest(
            @AuthenticationPrincipal String email
    ) {
        return ResponseEntity.ok(analysisService.getLatest(email));
    }
}