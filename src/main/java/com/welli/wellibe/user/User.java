package com.welli.wellibe.user;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 로그인에 사용하는 이메일
    @Column(nullable = false, unique = true)
    private String email;

    // 암호화해서 저장할 비밀번호
    @Column(nullable = false)
    private String password;

    // 닉네임
    private String nickname;

    // 온보딩에서 입력받는 나이
    private Integer age;

    // 성별
    private String gender;

    // 건강 목표
    @Enumerated(EnumType.STRING)
    private HealthGoal healthGoal;

    // 온보딩 완료 여부
    @Builder.Default
    @Column(nullable = false)
    private boolean onboardingCompleted = false;

    // 회원가입 시간
    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
    public void updateProfile(
            Integer age,
            String gender,
            HealthGoal healthGoal
    ) {
        this.age = age;
        this.gender = gender;
        this.healthGoal = healthGoal;
        this.onboardingCompleted = true;
    }
}
