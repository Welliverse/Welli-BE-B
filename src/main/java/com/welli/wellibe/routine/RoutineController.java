package com.welli.wellibe.routine;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/routines")
@RequiredArgsConstructor
public class RoutineController {

    private final RoutineService routineService;

    @GetMapping("/recommendations")
    public ResponseEntity<List<RoutineResponse>> getRecommendations(
            @AuthenticationPrincipal String email
    ) {
        return ResponseEntity.ok(
                routineService.getRecommendations(email)
        );
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<RoutineResponse> complete(
            @AuthenticationPrincipal String email,
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                routineService.complete(email, id)
        );
    }
}