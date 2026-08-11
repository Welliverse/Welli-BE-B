package com.welli.wellibe.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginResponse {

    private String accessToken;
    private Long userId;
    private String email;
    private String nickname;
}