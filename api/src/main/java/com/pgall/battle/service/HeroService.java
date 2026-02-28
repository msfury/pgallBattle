package com.pgall.battle.service;

import com.pgall.battle.entity.Equipment;
import com.pgall.battle.entity.GameCharacter;
import com.pgall.battle.enums.CharacterClass;
import com.pgall.battle.enums.EquipmentType;
import com.pgall.battle.repository.EquipmentRepository;
import com.pgall.battle.repository.GameCharacterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Slf4j
public class HeroService {

    private static final String HERO_SUFFIX = "용사";
    private static final String HERO_IP_PREFIX = "999.999.999.99";
    private static final int INITIAL_GACHA_COUNT = 50;
    private static final int DAILY_GACHA_COUNT = 5;
    private static final int HERO_GOLD = 10000;

    // 슬롯 구성: WEAPON x2, HELMET x1, ARMOR x1, GLOVES x1, SHOES x1, EARRING x2, RING x2
    private static final List<EquipmentType> ALL_SLOT_TYPES = List.of(
            EquipmentType.WEAPON, EquipmentType.WEAPON,
            EquipmentType.HELMET, EquipmentType.ARMOR,
            EquipmentType.GLOVES, EquipmentType.SHOES,
            EquipmentType.EARRING, EquipmentType.EARRING,
            EquipmentType.RING, EquipmentType.RING
    );

    private final GameCharacterRepository characterRepository;
    private final EquipmentRepository equipmentRepository;
    private final GachaService gachaService;

    /** 서버 시작 시 용사가 없으면 생성 */
    @Transactional
    public void initHeroes() {
        List<GameCharacter> existingHeroes = characterRepository.findByNameContaining(HERO_SUFFIX);
        if (!existingHeroes.isEmpty()) {
            log.info("용사 캐릭터 {}명 이미 존재합니다.", existingHeroes.size());
            return;
        }

        log.info("용사 캐릭터를 초기 생성합니다...");
        int heroIndex = 1;
        for (CharacterClass charClass : CharacterClass.values()) {
            GameCharacter hero = createHero(charClass, heroIndex++);
            initialGachaAndEquip(hero);
            log.info("  {} 생성 완료 (ID: {})", hero.getName(), hero.getId());
        }
        log.info("용사 캐릭터 {}명 생성 완료.", CharacterClass.values().length);
    }

    /** 매일 0시: 모든 용사에게 가챠 5회 + 자동 장착 */
    @Transactional
    public void dailyHeroGacha() {
        List<GameCharacter> heroes = characterRepository.findByNameContaining(HERO_SUFFIX);
        if (heroes.isEmpty()) return;

        log.info("용사 일일 가챠 시작 ({}명)...", heroes.size());
        for (GameCharacter hero : heroes) {
            // 아바타 수정 (구 숫자 ID → 클래스별 아바타)
            if (hero.getAvatar() == null || !hero.getAvatar().contains("_")) {
                hero.setAvatar(pickAvatarForClass(hero.getCharacterClass(), ThreadLocalRandom.current()));
            }
            // 5번 가챠
            for (int i = 0; i < DAILY_GACHA_COUNT; i++) {
                gachaService.pullFree(hero);
            }
            // 최적 장비 장착 + 나머지 삭제
            autoEquipBest(hero);
            deleteUnequipped(hero);
            characterRepository.save(hero);
        }
        log.info("용사 일일 가챠 완료.");
    }

    private GameCharacter createHero(CharacterClass charClass, int index) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        GameCharacter hero = GameCharacter.builder()
                .name(charClass.getKoreanName() + " " + HERO_SUFFIX)
                .avatar(pickAvatarForClass(charClass, random))
                .characterClass(charClass)
                .ipAddress(HERO_IP_PREFIX + index)
                .strength(roll4d6DropLowest(random))
                .dexterity(roll4d6DropLowest(random))
                .constitution(roll4d6DropLowest(random))
                .intelligence(roll4d6DropLowest(random))
                .wisdom(roll4d6DropLowest(random))
                .charisma(roll4d6DropLowest(random))
                .gold(HERO_GOLD)
                .build();
        return characterRepository.save(hero);
    }

    private void initialGachaAndEquip(GameCharacter hero) {
        // 50번 가챠
        for (int i = 0; i < INITIAL_GACHA_COUNT; i++) {
            gachaService.pullFree(hero);
        }

        // 최적 장비 장착
        autoEquipBest(hero);

        // 빈 슬롯 확인 후 해당 타입 가챠로 채우기
        Set<EquipmentType> missingTypes = findMissingSlotTypes(hero);
        for (EquipmentType missingType : missingTypes) {
            Equipment eq = gachaService.pullFreeForType(hero, missingType);
            eq.setEquipped(true);
            equipmentRepository.save(eq);
        }

        // 남은 미장착 장비 삭제
        deleteUnequipped(hero);
        characterRepository.save(hero);
    }

    /** 모든 슬롯에 최고 등급 장비를 장착 */
    private void autoEquipBest(GameCharacter hero) {
        // 먼저 전부 해제
        List<Equipment> allEquip = equipmentRepository.findByCharacterId(hero.getId());
        for (Equipment eq : allEquip) {
            eq.setEquipped(false);
        }
        equipmentRepository.saveAll(allEquip);

        // 타입별 장비 목록 (등급 높은 순, 그 다음 공+방 높은 순)
        Map<EquipmentType, List<Equipment>> byType = new LinkedHashMap<>();
        for (Equipment eq : allEquip) {
            byType.computeIfAbsent(eq.getType(), k -> new ArrayList<>()).add(eq);
        }
        byType.values().forEach(list -> list.sort(
                Comparator.comparingInt((Equipment e) -> e.getGrade().ordinal()).reversed()
                        .thenComparingInt((Equipment e) -> e.getAttackBonus() + e.getDefenseBonus()).reversed()
        ));

        // 슬롯별 장착
        Map<EquipmentType, Integer> equipped = new HashMap<>();
        boolean hasTwoHandedWeapon = false;
        for (EquipmentType slotType : ALL_SLOT_TYPES) {
            int count = equipped.getOrDefault(slotType, 0);
            // 양손무기가 장착되어 있으면 두번째 무기 슬롯 스킵
            if (slotType == EquipmentType.WEAPON && hasTwoHandedWeapon) continue;
            List<Equipment> candidates = byType.getOrDefault(slotType, List.of());
            if (count < candidates.size()) {
                Equipment best = candidates.get(count);
                // 양손무기인데 이미 한손무기가 장착되어 있으면 스킵
                if (slotType == EquipmentType.WEAPON && best.isTwoHanded() && count > 0) continue;
                best.setEquipped(true);
                equipmentRepository.save(best);
                equipped.put(slotType, count + 1);
                if (slotType == EquipmentType.WEAPON && best.isTwoHanded()) hasTwoHandedWeapon = true;
            }
        }
    }

    /** 장착되지 않은 슬롯 타입 찾기 */
    private Set<EquipmentType> findMissingSlotTypes(GameCharacter hero) {
        List<Equipment> equippedItems = equipmentRepository.findByCharacterIdAndEquipped(hero.getId(), true);
        Map<EquipmentType, Integer> equippedCount = new HashMap<>();
        for (Equipment eq : equippedItems) {
            equippedCount.merge(eq.getType(), 1, Integer::sum);
        }

        Set<EquipmentType> missing = new LinkedHashSet<>();
        Map<EquipmentType, Integer> needed = new HashMap<>();
        for (EquipmentType slotType : ALL_SLOT_TYPES) {
            needed.merge(slotType, 1, Integer::sum);
        }

        for (Map.Entry<EquipmentType, Integer> entry : needed.entrySet()) {
            int have = equippedCount.getOrDefault(entry.getKey(), 0);
            for (int i = have; i < entry.getValue(); i++) {
                missing.add(entry.getKey());
            }
        }
        return missing;
    }

    private void deleteUnequipped(GameCharacter hero) {
        List<Equipment> unequipped = equipmentRepository.findByCharacterIdAndEquipped(hero.getId(), false);
        equipmentRepository.deleteAll(unequipped);
    }

    private String pickAvatarForClass(CharacterClass charClass, ThreadLocalRandom random) {
        return switch (charClass) {
            case WARRIOR -> {
                String[] options = {"warrior_1", "warrior_2", "warrior_3"};
                yield options[random.nextInt(options.length)];
            }
            case MAGE -> "mage_1";
            case RANGER -> {
                String[] options = {"ranger_1", "ranger_2"};
                yield options[random.nextInt(options.length)];
            }
            case ROGUE -> "rogue_1";
            case CLERIC -> "cleric_1";
        };
    }

    private int roll4d6DropLowest(ThreadLocalRandom random) {
        int[] rolls = new int[4];
        for (int i = 0; i < 4; i++) rolls[i] = random.nextInt(1, 7);
        Arrays.sort(rolls);
        return rolls[1] + rolls[2] + rolls[3];
    }
}
