package com.welli.wellibe.character;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CharacterRepository
        extends JpaRepository<Character, Long> {

    Optional<Character> findByUserId(Long userId);

    boolean existsByUserId(Long userId);
}