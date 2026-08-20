package com.welli.wellibe.character;

import com.welli.wellibe.user.User;
import com.welli.wellibe.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CharacterService {

    private final CharacterRepository characterRepository;
    private final UserRepository userRepository;

    @Transactional
    public CharacterResponse create(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new IllegalArgumentException("존재하지 않는 사용자입니다.")
                );

        if (characterRepository.existsByUserId(user.getId())) {
            throw new IllegalArgumentException("이미 캐릭터가 존재합니다.");
        }

        Character character = Character.builder()
                .user(user)
                .growthStage(1)
                .growthScore(0)
                .conditionScore(50)
                .appearanceState("NORMAL")
                .build();

        Character savedCharacter = characterRepository.save(character);

        return toResponse(savedCharacter);
    }

    @Transactional(readOnly = true)
    public CharacterResponse getMyCharacter(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new IllegalArgumentException("존재하지 않는 사용자입니다.")
                );

        Character character = characterRepository.findByUserId(user.getId())
                .orElseThrow(() ->
                        new IllegalArgumentException("캐릭터가 존재하지 않습니다.")
                );

        return toResponse(character);
    }

    private CharacterResponse toResponse(Character character) {
        return new CharacterResponse(
                character.getId(),
                character.getGrowthStage(),
                character.getGrowthScore(),
                character.getConditionScore(),
                character.getAppearanceState()
        );
    }
}
