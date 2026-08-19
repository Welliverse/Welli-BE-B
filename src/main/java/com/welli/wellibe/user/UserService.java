package com.welli.wellibe.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public UserResponse getMyInfo(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new IllegalArgumentException("존재하지 않는 사용자입니다.")
                );

        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getAge(),
                user.getGender(),
                user.getHealthGoal(),
                user.isOnboardingCompleted()
        );
    }
    @Transactional
    public void updateProfile(
            String email,
            ProfileUpdateRequest request
    ) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new IllegalArgumentException("존재하지 않는 사용자입니다.")
                );

        user.updateProfile(
                request.age(),
                request.gender(),
                request.healthGoal()
        );
    }
}
