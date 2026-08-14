package com.welli.wellibe.routine;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RoutineRecommendationRepository
        extends JpaRepository<RoutineRecommendation, Long> {

    List<RoutineRecommendation> findByUserIdAndRecommendedAtOrderByPriorityAsc(
            Long userId,
            LocalDate recommendedAt
    );

    Optional<RoutineRecommendation> findByIdAndUserId(
            Long id,
            Long userId
    );
}