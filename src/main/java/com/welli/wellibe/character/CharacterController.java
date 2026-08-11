package com.welli.wellibe.character;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/characters")
@RequiredArgsConstructor
public class CharacterController {

    private final CharacterService characterService;

    @PostMapping
    public ResponseEntity<CharacterResponse> create(
            @AuthenticationPrincipal String email
    ) {
        return ResponseEntity.ok(
                characterService.create(email)
        );
    }

    @GetMapping("/me")
    public ResponseEntity<CharacterResponse> getMyCharacter(
            @AuthenticationPrincipal String email
    ) {
        return ResponseEntity.ok(
                characterService.getMyCharacter(email)
        );
    }
}