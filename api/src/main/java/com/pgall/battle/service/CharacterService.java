package com.pgall.battle.service;

import com.pgall.battle.dto.*;
import com.pgall.battle.entity.GameCharacter;
import com.pgall.battle.enums.CharacterClass;
import com.pgall.battle.repository.BattleLogRepository;
import com.pgall.battle.repository.GameCharacterRepository;
import com.pgall.battle.repository.InventoryRepository;
import com.pgall.battle.repository.ShopItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CharacterService {

    private final GameCharacterRepository characterRepository;
    private final BattleLogRepository battleLogRepository;
    private final InventoryRepository inventoryRepository;
    private final ShopItemRepository shopItemRepository;

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
    public CharacterResponse createCharacter(CharacterCreateRequest request, String ip) {
        // 이미 해당 IP로 생성된 캐릭터가 있으면 차단
        characterRepository.findByIpAddress(ip).ifPresent(c -> {
            throw new IllegalStateException("이미 캐릭터가 존재합니다: " + c.getName());
        });

        CharacterClass charClass = null;
        if (request.getCharacterClass() != null && !request.getCharacterClass().isBlank()) {
            charClass = CharacterClass.valueOf(request.getCharacterClass());
        }

        GameCharacter character = GameCharacter.builder()
                .name(request.getName())
                .avatar(request.getAvatar())
                .characterClass(charClass)
                .ipAddress(ip)
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

    /** 해당 IP로 생성된 내 캐릭터 조회 */
    public CharacterResponse getMyCharacter(String ip) {
        GameCharacter character = characterRepository.findByIpAddress(ip)
                .orElseThrow(() -> new NoSuchElementException("캐릭터가 없습니다."));
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

    @Transactional
    public void deleteCharacter(Long id) {
        GameCharacter character = characterRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("캐릭터를 찾을 수 없습니다: " + id));

        // 인벤토리의 ShopItem 삭제
        var inventories = inventoryRepository.findByCharacterId(id);
        var shopItemIds = inventories.stream().map(inv -> inv.getShopItem().getId()).toList();
        inventoryRepository.deleteAll(inventories);
        shopItemRepository.deleteAllById(shopItemIds);

        // 전투 기록 삭제
        var battleLogs = battleLogRepository.findByAttackerIdOrDefenderIdOrderByCreatedAtDesc(id, id);
        battleLogRepository.deleteAll(battleLogs);

        // 캐릭터 삭제 (equipment는 cascade로 자동 삭제)
        characterRepository.delete(character);
    }

    @Transactional
    public boolean checkAndGrantDailyGold(Long characterId) {
        GameCharacter character = characterRepository.findById(characterId)
                .orElseThrow(() -> new NoSuchElementException("캐릭터를 찾을 수 없습니다."));
        LocalDate today = LocalDate.now();
        if (today.equals(character.getLastDailyGoldDate())) {
            return false;
        }
        character.setGold(character.getGold() + 300);
        character.setLastDailyGoldDate(today);
        characterRepository.save(character);
        return true;
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
