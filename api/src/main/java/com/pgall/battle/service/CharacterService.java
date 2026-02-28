package com.pgall.battle.service;

import com.pgall.battle.dto.*;
import com.pgall.battle.entity.GameCharacter;
import com.pgall.battle.enums.CharacterClass;
import com.pgall.battle.repository.GameCharacterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CharacterService {

    private final GameCharacterRepository characterRepository;

    public RandomStatsResponse generateRandomStats() {
        return RandomStatsResponse.builder()
                .strength(roll4d6DropLowest())
                .dexterity(roll4d6DropLowest())
                .constitution(roll4d6DropLowest())
                .intelligence(roll4d6DropLowest())
                .wisdom(roll4d6DropLowest())
                .charisma(roll4d6DropLowest())
                .build();
    }

    @Transactional
    public CharacterResponse createCharacter(CharacterCreateRequest request) {
        CharacterClass charClass = null;
        if (request.getCharacterClass() != null && !request.getCharacterClass().isBlank()) {
            charClass = CharacterClass.valueOf(request.getCharacterClass());
        }

        GameCharacter character = GameCharacter.builder()
                .name(request.getName())
                .avatar(request.getAvatar())
                .characterClass(charClass)
                .strength(request.getStrength())
                .dexterity(request.getDexterity())
                .constitution(request.getConstitution())
                .intelligence(request.getIntelligence())
                .wisdom(request.getWisdom())
                .charisma(request.getCharisma())
                .build();

        character = characterRepository.save(character);
        return CharacterResponse.from(character);
    }

    public List<CharacterResponse> getAllCharacters() {
        return characterRepository.findAll().stream()
                .map(CharacterResponse::from)
                .toList();
    }

    public CharacterResponse getCharacter(Long id) {
        GameCharacter character = characterRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("캐릭터를 찾을 수 없습니다: " + id));
        return CharacterResponse.from(character);
    }

    private int roll4d6DropLowest() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int[] rolls = new int[4];
        for (int i = 0; i < 4; i++) {
            rolls[i] = random.nextInt(1, 7);
        }
        Arrays.sort(rolls);
        return rolls[1] + rolls[2] + rolls[3];
    }
}
