package com.welli.wellibe.user;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserResponse {

    private Long userId;
    private String email;
    private String nickname;
    private Integer age;
    private String gender;
    private HealthGoal healthGoal;
}
