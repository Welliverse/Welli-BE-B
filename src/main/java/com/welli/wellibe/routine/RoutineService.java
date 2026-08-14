package com.welli.wellibe.routine;

import com.welli.wellibe.character.Character;
import com.welli.wellibe.character.CharacterRepository;
import com.welli.wellibe.user.User;
import com.welli.wellibe.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RoutineService {

    private final RoutineRecommendationRepository routineRepository;
    private final UserRepository userRepository;
    private final CharacterRepository characterRepository;

    @Transactional
    public List<RoutineResponse> getRecommendations(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new IllegalArgumentException("존재하지 않는 사용자입니다.")
                );

        LocalDate today = LocalDate.now();

        List<RoutineRecommendation> routines = routineRepository
                .findByUserIdAndRecommendedAtOrderByPriorityAsc(
                        user.getId(),
                        today
                );

        if (routines.isEmpty()) {
            Character character = characterRepository.findByUserId(user.getId())
                    .orElseThrow(() ->
                            new IllegalArgumentException("캐릭터가 존재하지 않습니다.")
                    );

            routines = createTodayRoutines(user, character, today);
        }

        return routines.stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public RoutineResponse complete(String email, Long routineId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new IllegalArgumentException("존재하지 않는 사용자입니다.")
                );

        RoutineRecommendation routine = routineRepository
                .findByIdAndUserId(routineId, user.getId())
                .orElseThrow(() ->
                        new IllegalArgumentException("루틴이 존재하지 않습니다.")
                );

        if (routine.isCompleted()) {
            throw new IllegalArgumentException("이미 완료한 루틴입니다.");
        }

        routine.complete();

        Character character = characterRepository.findByUserId(user.getId())
                .orElseThrow(() ->
                        new IllegalArgumentException("캐릭터가 존재하지 않습니다.")
                );

        character.updateCondition(character.getConditionScore() + 5);

        return toResponse(routine);
    }

    private List<RoutineRecommendation> createTodayRoutines(
            User user,
            Character character,
            LocalDate today
    ) {
        List<RoutineRecommendation> routines;

        if (character.getConditionScore() < 60) {
            routines = List.of(
                    RoutineRecommendation.builder()
                            .user(user)
                            .routineType(RoutineType.SLEEP)
                            .priority(1)
                            .recommendedAt(today)
                            .isCompleted(false)
                            .build(),
                    RoutineRecommendation.builder()
                            .user(user)
                            .routineType(RoutineType.WATER)
                            .priority(2)
                            .recommendedAt(today)
                            .isCompleted(false)
                            .build(),
                    RoutineRecommendation.builder()
                            .user(user)
                            .routineType(RoutineType.MEDITATION)
                            .priority(3)
                            .recommendedAt(today)
                            .isCompleted(false)
                            .build()
            );
        } else {
            routines = List.of(
                    RoutineRecommendation.builder()
                            .user(user)
                            .routineType(RoutineType.WATER)
                            .priority(1)
                            .recommendedAt(today)
                            .isCompleted(false)
                            .build(),
                    RoutineRecommendation.builder()
                            .user(user)
                            .routineType(RoutineType.EXERCISE)
                            .priority(2)
                            .recommendedAt(today)
                            .isCompleted(false)
                            .build(),
                    RoutineRecommendation.builder()
                            .user(user)
                            .routineType(RoutineType.SKINCARE)
                            .priority(3)
                            .recommendedAt(today)
                            .isCompleted(false)
                            .build()
            );
        }

        return routineRepository.saveAll(routines);
    }

    private RoutineResponse toResponse(RoutineRecommendation routine) {
        return new RoutineResponse(
                routine.getId(),
                routine.getRoutineType(),
                routine.getPriority(),
                routine.getRecommendedAt(),
                routine.isCompleted()
        );
    }
}