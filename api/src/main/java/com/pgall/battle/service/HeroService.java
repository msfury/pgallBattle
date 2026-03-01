package com.pgall.battle.service;

import com.pgall.battle.entity.Equipment;
import com.pgall.battle.entity.GameCharacter;
import com.pgall.battle.enums.*;
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
    private static final int HERO_STAT_TOTAL_MIN = 100;

    // 슬롯 구성: WEAPON x2, HELMET x1, ARMOR x1, GLOVES x1, SHOES x1, EARRING x2, RING x2
    private static final List<EquipmentType> ALL_SLOT_TYPES = List.of(
            EquipmentType.WEAPON, EquipmentType.WEAPON,
            EquipmentType.HELMET, EquipmentType.ARMOR,
            EquipmentType.GLOVES, EquipmentType.SHOES,
            EquipmentType.EARRING, EquipmentType.EARRING,
            EquipmentType.RING, EquipmentType.RING
    );

    // 클래스별 전용 무기
    private static final Map<CharacterClass, WeaponCategory[]> CLASS_WEAPONS = Map.of(
            CharacterClass.WARRIOR, new WeaponCategory[]{WeaponCategory.GREATSWORD},
            CharacterClass.ROGUE, new WeaponCategory[]{WeaponCategory.DAGGER},
            CharacterClass.MAGE, new WeaponCategory[]{WeaponCategory.STAFF},
            CharacterClass.CLERIC, new WeaponCategory[]{WeaponCategory.STAFF, WeaponCategory.MACE},
            CharacterClass.RANGER, new WeaponCategory[]{WeaponCategory.BOW}
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
            log.info("  {} 생성 완료 (ID: {}, 능력치합: {})", hero.getName(), hero.getId(), statTotal(hero));
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
            if (hero.getAvatar() == null || !hero.getAvatar().contains("_")) {
                hero.setAvatar(pickAvatarForClass(hero.getCharacterClass(), ThreadLocalRandom.current()));
            }
            for (int i = 0; i < DAILY_GACHA_COUNT; i++) {
                gachaService.pullFree(hero);
            }
            autoEquipBest(hero);
            ensureClassWeapon(hero);
            deleteUnequipped(hero);
            characterRepository.save(hero);
        }
        log.info("용사 일일 가챠 완료.");
    }

    private GameCharacter createHero(CharacterClass charClass, int index) {
        ThreadLocalRandom random = ThreadLocalRandom.current();

        // 6개 능력치 굴림
        int[] stats = new int[6]; // STR, DEX, CON, INT, WIS, CHA
        for (int i = 0; i < 6; i++) stats[i] = roll4d6DropLowest(random);

        // 메인 스탯 위치에 최고치 배치
        int mainIdx = getMainStatIndex(charClass);
        int maxIdx = 0;
        for (int i = 1; i < 6; i++) if (stats[i] > stats[maxIdx]) maxIdx = i;
        if (maxIdx != mainIdx) {
            int tmp = stats[mainIdx]; stats[mainIdx] = stats[maxIdx]; stats[maxIdx] = tmp;
        }

        // 총합 100 이상 보장 (메인스탯 우선으로 부스트, 각 스탯 최대 20)
        int total = Arrays.stream(stats).sum();
        while (total < HERO_STAT_TOTAL_MIN) {
            int idx = random.nextInt(100) < 60 ? mainIdx : random.nextInt(6);
            if (stats[idx] < 20) { stats[idx]++; total++; }
        }

        GameCharacter hero = GameCharacter.builder()
                .name(charClass.getKoreanName() + " " + HERO_SUFFIX)
                .avatar(pickAvatarForClass(charClass, random))
                .characterClass(charClass)
                .ipAddress(HERO_IP_PREFIX + index)
                .strength(stats[0]).dexterity(stats[1]).constitution(stats[2])
                .intelligence(stats[3]).wisdom(stats[4]).charisma(stats[5])
                .gold(HERO_GOLD)
                .build();
        return characterRepository.save(hero);
    }

    /** 클래스별 메인 스탯 인덱스 (STR=0, DEX=1, CON=2, INT=3, WIS=4, CHA=5) */
    private int getMainStatIndex(CharacterClass charClass) {
        return switch (charClass) {
            case WARRIOR -> 0; // STR
            case ROGUE, RANGER -> 1; // DEX
            case MAGE -> 3; // INT
            case CLERIC -> 4; // WIS
        };
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
            Equipment eq = gachaService.pullFreeForTypeWithMinGrade(hero, missingType, EquipmentGrade.RARE);
            eq.setEquipped(true);
            equipmentRepository.save(eq);
        }

        // 부위별 RARE 미만 장비를 RARE 이상으로 교체
        upgradeToMinRare(hero);

        // 클래스 전용 무기 보장
        ensureClassWeapon(hero);

        // 남은 미장착 장비 삭제
        deleteUnequipped(hero);
        characterRepository.save(hero);
    }

    /** 클래스 전용 무기가 장착되어 있지 않으면 생성하여 교체 */
    private void ensureClassWeapon(GameCharacter hero) {
        WeaponCategory[] allowed = CLASS_WEAPONS.get(hero.getCharacterClass());
        if (allowed == null) return;

        List<Equipment> equippedWeapons = equipmentRepository.findByCharacterIdAndEquipped(hero.getId(), true)
                .stream().filter(e -> e.getType() == EquipmentType.WEAPON).toList();

        boolean hasClassWeapon = equippedWeapons.stream()
                .anyMatch(e -> e.getWeaponCategory() != null && Arrays.asList(allowed).contains(e.getWeaponCategory()));

        if (!hasClassWeapon) {
            // 기존 무기 해제 + 삭제
            for (Equipment w : equippedWeapons) {
                w.setEquipped(false);
                equipmentRepository.save(w);
                equipmentRepository.delete(w);
            }
            // 클래스 전용 무기 RARE+ 생성
            ThreadLocalRandom random = ThreadLocalRandom.current();
            WeaponCategory category = allowed[random.nextInt(allowed.length)];
            Equipment weapon = gachaService.pullFreeForWeaponCategory(hero, category, EquipmentGrade.RARE);
            weapon.setEquipped(true);
            equipmentRepository.save(weapon);
        }
    }

    /** 장착된 장비 중 RARE 미만을 RARE 이상으로 교체 */
    private void upgradeToMinRare(GameCharacter hero) {
        List<Equipment> equippedItems = equipmentRepository.findByCharacterIdAndEquipped(hero.getId(), true);
        for (Equipment eq : equippedItems) {
            if (eq.getGrade().ordinal() < EquipmentGrade.RARE.ordinal()) {
                eq.setEquipped(false);
                equipmentRepository.save(eq);
                equipmentRepository.delete(eq);
                Equipment newEq = gachaService.pullFreeForTypeWithMinGrade(hero, eq.getType(), EquipmentGrade.RARE);
                newEq.setEquipped(true);
                equipmentRepository.save(newEq);
            }
        }
    }

    /** 모든 슬롯에 최고 등급 장비를 장착 */
    private void autoEquipBest(GameCharacter hero) {
        List<Equipment> allEquip = equipmentRepository.findByCharacterId(hero.getId());
        for (Equipment eq : allEquip) eq.setEquipped(false);
        equipmentRepository.saveAll(allEquip);

        Map<EquipmentType, List<Equipment>> byType = new LinkedHashMap<>();
        for (Equipment eq : allEquip) {
            byType.computeIfAbsent(eq.getType(), k -> new ArrayList<>()).add(eq);
        }
        byType.values().forEach(list -> list.sort(
                Comparator.comparingInt((Equipment e) -> e.getGrade().ordinal()).reversed()
                        .thenComparingInt((Equipment e) -> e.getAttackBonus() + e.getDefenseBonus()).reversed()
        ));

        Map<EquipmentType, Integer> equipped = new HashMap<>();
        boolean hasTwoHandedWeapon = false;
        for (EquipmentType slotType : ALL_SLOT_TYPES) {
            int count = equipped.getOrDefault(slotType, 0);
            if (slotType == EquipmentType.WEAPON && hasTwoHandedWeapon) continue;
            List<Equipment> candidates = byType.getOrDefault(slotType, List.of());
            if (count < candidates.size()) {
                Equipment best = candidates.get(count);
                if (slotType == EquipmentType.WEAPON && best.isTwoHanded() && count > 0) continue;
                best.setEquipped(true);
                equipmentRepository.save(best);
                equipped.put(slotType, count + 1);
                if (slotType == EquipmentType.WEAPON && best.isTwoHanded()) hasTwoHandedWeapon = true;
            }
        }
    }

    private Set<EquipmentType> findMissingSlotTypes(GameCharacter hero) {
        List<Equipment> equippedItems = equipmentRepository.findByCharacterIdAndEquipped(hero.getId(), true);
        Map<EquipmentType, Integer> equippedCount = new HashMap<>();
        for (Equipment eq : equippedItems) equippedCount.merge(eq.getType(), 1, Integer::sum);

        Set<EquipmentType> missing = new LinkedHashSet<>();
        Map<EquipmentType, Integer> needed = new HashMap<>();
        for (EquipmentType slotType : ALL_SLOT_TYPES) needed.merge(slotType, 1, Integer::sum);

        for (Map.Entry<EquipmentType, Integer> entry : needed.entrySet()) {
            int have = equippedCount.getOrDefault(entry.getKey(), 0);
            for (int i = have; i < entry.getValue(); i++) missing.add(entry.getKey());
        }
        return missing;
    }

    private void deleteUnequipped(GameCharacter hero) {
        equipmentRepository.deleteAll(equipmentRepository.findByCharacterIdAndEquipped(hero.getId(), false));
    }

    private int statTotal(GameCharacter h) {
        return h.getStrength() + h.getDexterity() + h.getConstitution()
                + h.getIntelligence() + h.getWisdom() + h.getCharisma();
    }

    private String pickAvatarForClass(CharacterClass charClass, ThreadLocalRandom random) {
        return switch (charClass) {
            case WARRIOR -> { String[] o = {"warrior_1","warrior_2","warrior_3"}; yield o[random.nextInt(o.length)]; }
            case MAGE -> "mage_1";
            case RANGER -> { String[] o = {"ranger_1","ranger_2"}; yield o[random.nextInt(o.length)]; }
            case ROGUE -> { String[] o = {"rogue_1","rogue_2"}; yield o[random.nextInt(o.length)]; }
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
