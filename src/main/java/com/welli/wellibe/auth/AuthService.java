package com.welli.wellibe.auth;

import com.welli.wellibe.user.User;
import com.welli.wellibe.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.welli.wellibe.global.jwt.JwtTokenProvider;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public SignupResponse signup(SignupRequest request) {

        // 이메일 중복 확인
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("이미 가입된 이메일입니다.");
        }

        // 사용자 생성
        User user = User.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .nickname(request.nickname())
                .build();

        // DB 저장
        User savedUser = userRepository.save(user);

        return new SignupResponse(
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getNickname()
        );
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {

        // 이메일로 사용자 조회
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() ->
                        new IllegalArgumentException("존재하지 않는 이메일입니다.")
                );

        // 비밀번호 확인
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        // 로그인 응답
        String accessToken = jwtTokenProvider.createToken(
                user.getId(),
                user.getEmail()
        );

        return new LoginResponse(
                accessToken,
                user.getId(),
                user.getEmail(),
                user.getNickname()
        );
    }
}
