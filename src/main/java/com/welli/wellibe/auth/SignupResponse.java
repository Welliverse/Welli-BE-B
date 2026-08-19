package com.welli.wellibe.auth;

public record SignupResponse(
        Long userId,
        String email,
        String nickname
) {
}
