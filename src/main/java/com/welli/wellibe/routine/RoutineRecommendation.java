package com.welli.wellibe.routine;

import com.welli.wellibe.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "routine_recommendations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class RoutineRecommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoutineType routineType;

    @Column(nullable = false)
    private Integer priority;

    @Column(nullable = false)
    private LocalDate recommendedAt;

    @Column(nullable = false)
    private boolean isCompleted;

    public void complete() {
        this.isCompleted = true;
    }
}