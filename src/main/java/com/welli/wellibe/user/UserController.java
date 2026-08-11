package com.welli.wellibe.user;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMyInfo(
            @AuthenticationPrincipal String email
    ) {
        return ResponseEntity.ok(userService.getMyInfo(email));
    }

    @PatchMapping("/me/profile")
    public ResponseEntity<Void> updateProfile(
            @AuthenticationPrincipal String email,
            @Valid @RequestBody ProfileUpdateRequest request
    ) {
        userService.updateProfile(email, request);

        return ResponseEntity.noContent().build();
    }
}
