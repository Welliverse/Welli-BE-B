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
}