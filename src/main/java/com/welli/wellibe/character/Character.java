package com.welli.wellibe.character;

import com.welli.wellibe.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "characters")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Character {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    private Integer growthStage;

    private Integer conditionScore;

    private String appearanceState;

    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void updateTime() {
        this.updatedAt = LocalDateTime.now();
    }
    public void updateCondition(int conditionScore) {
        this.conditionScore = Math.max(0, Math.min(100, conditionScore));

        if (this.conditionScore >= 80) {
            this.appearanceState = "VERY_GOOD";
        } else if (this.conditionScore >= 60) {
            this.appearanceState = "GOOD";
        } else if (this.conditionScore >= 40) {
            this.appearanceState = "NORMAL";
        } else if (this.conditionScore >= 20) {
            this.appearanceState = "BAD";
        } else {
            this.appearanceState = "VERY_BAD";
        }
    }
}