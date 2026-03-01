package com.pgall.battle.service;

import com.pgall.battle.data.WeaponNameData;
import com.pgall.battle.dto.EquipmentResponse;
import com.pgall.battle.entity.BaseEffect;
import com.pgall.battle.entity.Equipment;
import com.pgall.battle.entity.GameCharacter;
import com.pgall.battle.enums.*;
import com.pgall.battle.repository.EquipmentRepository;
import com.pgall.battle.repository.GameCharacterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class GachaService {

    private static final int GACHA_COST = 30;

    private final GameCharacterRepository characterRepository;
    private final EquipmentRepository equipmentRepository;

    // 슬롯별 효과 풀
    private static final List<EquipmentEffect> WEAPON_EFFECTS =
            Arrays.stream(EquipmentEffect.values())
                    .filter(e -> e.getCategory() == EquipmentEffect.Category.WEAPON).toList();

    private static final List<EquipmentEffect> ARMOR_EFFECTS =
            Arrays.stream(EquipmentEffect.values())
                    .filter(e -> e.getCategory() == EquipmentEffect.Category.ARMOR).toList();

    private static final List<EquipmentEffect> ACCESSORY_EFFECTS =
            List.of(EquipmentEffect.values()); // 악세사리는 모든 효과 가능

    @Transactional
    public EquipmentResponse pull(Long characterId) {
        GameCharacter character = characterRepository.findById(characterId)
                .orElseThrow(() -> new NoSuchElementException("캐릭터를 찾을 수 없습니다."));

        if (character.getGold() < GACHA_COST) {
            throw new IllegalStateException("골드가 부족합니다. (필요: " + GACHA_COST + ", 보유: " + character.getGold() + ")");
        }

        character.setGold(character.getGold() - GACHA_COST);

        EquipmentGrade grade = rollGrade();
        EquipmentType type = rollType();
        Equipment equipment = generateEquipment(grade, type, character);

        equipmentRepository.save(equipment);
        characterRepository.save(character);

        return EquipmentResponse.from(equipment);
    }

    /** 골드 차감 없이 랜덤 가챠 (용사 시스템용) */
    @Transactional
    public Equipment pullFree(GameCharacter character) {
        EquipmentGrade grade = rollGrade();
        EquipmentType type = rollType();
        Equipment equipment = generateEquipment(grade, type, character);
        return equipmentRepository.save(equipment);
    }

    /** 골드 차감 없이 특정 타입 가챠 (용사 시스템용) */
    @Transactional
    public Equipment pullFreeForType(GameCharacter character, EquipmentType type) {
        EquipmentGrade grade = rollGrade();
        Equipment equipment = generateEquipment(grade, type, character);
        return equipmentRepository.save(equipment);
    }

    /** 골드 차감 없이 특정 타입 + 최소 등급 보장 가챠 (용사 RARE+ 보장용) */
    @Transactional
    public Equipment pullFreeForTypeWithMinGrade(GameCharacter character, EquipmentType type, EquipmentGrade minGrade) {
        EquipmentGrade grade;
        do {
            grade = rollGrade();
        } while (grade.ordinal() < minGrade.ordinal());
        Equipment equipment = generateEquipment(grade, type, character);
        return equipmentRepository.save(equipment);
    }

    /** 골드 차감 없이 특정 무기 카테고리 + 최소 등급 보장 가챠 (용사 전용 무기용) */
    @Transactional
    public Equipment pullFreeForWeaponCategory(GameCharacter character, WeaponCategory category, EquipmentGrade minGrade) {
        EquipmentGrade grade;
        do {
            grade = rollGrade();
        } while (grade.ordinal() < minGrade.ordinal());

        ThreadLocalRandom random = ThreadLocalRandom.current();
        int gradeMultiplier = grade.ordinal() + 1;
        String baseName = WeaponNameData.getRandomName(category, random);
        String name = getGradePrefix(grade) + " " + baseName;
        int dmgMin = category.getDiceCount() + (gradeMultiplier - 1);
        int dmgMax = category.getDiceCount() * category.getDiceSides() + gradeMultiplier;
        int atkBonus = gradeMultiplier + random.nextInt(0, gradeMultiplier);

        Equipment equipment = Equipment.builder()
                .type(EquipmentType.WEAPON).grade(grade).character(character)
                .name(name).attackBonus(atkBonus).defenseBonus(0)
                .weaponCategory(category).scalingStat(category.getScalingStat())
                .twoHanded(category.isTwoHanded())
                .baseDamageMin(dmgMin).baseDamageMax(dmgMax).build();

        int effectCount = getGradeEffectCount(grade);
        addBaseEffects(equipment, EquipmentType.WEAPON, grade, effectCount, random);

        return equipmentRepository.save(equipment);
    }

    private EquipmentGrade rollGrade() {
        int roll = ThreadLocalRandom.current().nextInt(100);
        if (roll < 1) return EquipmentGrade.LEGENDARY;
        if (roll < 6) return EquipmentGrade.EPIC;
        if (roll < 21) return EquipmentGrade.RARE;
        if (roll < 51) return EquipmentGrade.UNCOMMON;
        return EquipmentGrade.COMMON;
    }

    private EquipmentType rollType() {
        EquipmentType[] types = EquipmentType.values();
        return types[ThreadLocalRandom.current().nextInt(types.length)];
    }

    private WeaponCategory rollWeaponCategory() {
        WeaponCategory[] categories = WeaponCategory.values();
        return categories[ThreadLocalRandom.current().nextInt(categories.length)];
    }

    /** 클래스별 선호무기 50% 보정 */
    private WeaponCategory rollWeaponCategory(CharacterClass charClass) {
        if (charClass == null || charClass == CharacterClass.WARRIOR) return rollWeaponCategory();
        ThreadLocalRandom random = ThreadLocalRandom.current();
        if (random.nextBoolean()) {
            // 50% 선호무기 풀에서
            List<WeaponCategory> preferred = getPreferredWeapons(charClass);
            return preferred.get(random.nextInt(preferred.size()));
        }
        return rollWeaponCategory();
    }

    private List<WeaponCategory> getPreferredWeapons(CharacterClass charClass) {
        return switch (charClass) {
            case ROGUE -> List.of(WeaponCategory.DAGGER, WeaponCategory.CLAW, WeaponCategory.RAPIER);
            case MAGE -> List.of(WeaponCategory.STAFF, WeaponCategory.WAND);
            case RANGER -> List.of(WeaponCategory.BOW);
            case CLERIC -> List.of(WeaponCategory.MACE, WeaponCategory.FLAIL, WeaponCategory.WAND);
            case WARRIOR -> List.of(WeaponCategory.values());
        };
    }

    private Equipment generateEquipment(EquipmentGrade grade, EquipmentType type, GameCharacter character) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int gradeMultiplier = grade.ordinal() + 1;

        int atkBonus = 0;
        int defBonus = 0;
        String name;

        Equipment.EquipmentBuilder builder = Equipment.builder()
                .type(type)
                .grade(grade)
                .character(character);

        switch (type) {
            case WEAPON -> {
                WeaponCategory category = rollWeaponCategory(character.getCharacterClass());
                String baseName = WeaponNameData.getRandomName(category, random);
                name = getGradePrefix(grade) + " " + baseName;

                int dmgMin = category.getDiceCount() + (gradeMultiplier - 1);
                int dmgMax = category.getDiceCount() * category.getDiceSides() + gradeMultiplier;

                atkBonus = gradeMultiplier + random.nextInt(0, gradeMultiplier);

                builder.weaponCategory(category)
                       .scalingStat(category.getScalingStat())
                       .twoHanded(category.isTwoHanded())
                       .baseDamageMin(dmgMin)
                       .baseDamageMax(dmgMax);
            }
            case HELMET -> {
                defBonus = gradeMultiplier + random.nextInt(0, gradeMultiplier);
                name = getGradePrefix(grade) + " 투구";
            }
            case ARMOR -> {
                defBonus = gradeMultiplier + random.nextInt(1, gradeMultiplier + 1);
                name = getGradePrefix(grade) + " 갑옷";
            }
            case GLOVES -> {
                atkBonus = gradeMultiplier + random.nextInt(0, gradeMultiplier);
                name = getGradePrefix(grade) + " 장갑";
            }
            case SHOES -> {
                atkBonus = random.nextInt(0, gradeMultiplier + 1);
                name = getGradePrefix(grade) + " 신발";
            }
            case EARRING -> {
                atkBonus = gradeMultiplier;
                defBonus = gradeMultiplier;
                name = getGradePrefix(grade) + " 귀걸이";
            }
            case RING -> {
                atkBonus = gradeMultiplier;
                name = getGradePrefix(grade) + " 반지";
            }
            default -> name = getGradePrefix(grade) + " 장비";
        }

        builder.name(name)
               .attackBonus(atkBonus)
               .defenseBonus(defBonus);

        Equipment equipment = builder.build();

        // 등급별 기본 효과 개수: Common=0, Uncommon=1, Rare=2, Epic=3, Legendary=4
        int effectCount = getGradeEffectCount(grade);
        addBaseEffects(equipment, type, grade, effectCount, random);

        // 스탯 보너스: 언커먼 이상 또는 장갑/신발
        addStatBonuses(equipment, grade, type, random);

        return equipment;
    }

    /** 등급별 기본 효과 개수 */
    private int getGradeEffectCount(EquipmentGrade grade) {
        return switch (grade) {
            case COMMON -> 0;
            case UNCOMMON -> 1;
            case RARE -> 2;
            case EPIC -> 3;
            case LEGENDARY -> 4;
        };
    }

    /** 장비에 기본 효과를 등급별 개수만큼 추가 (중복 방지) */
    private void addBaseEffects(Equipment equipment, EquipmentType type, EquipmentGrade grade,
                                int count, ThreadLocalRandom random) {
        if (count <= 0) return;
        List<EquipmentEffect> pool = getEffectPool(type);
        java.util.Set<EquipmentEffect> used = new java.util.HashSet<>();

        for (int i = 0; i < count && used.size() < pool.size(); i++) {
            EquipmentEffect effect;
            do {
                effect = pool.get(random.nextInt(pool.size()));
            } while (used.contains(effect));
            used.add(effect);

            equipment.getBaseEffects().add(BaseEffect.builder()
                    .equipment(equipment)
                    .effect(effect)
                    .effectChance(getEffectChance(grade))
                    .effectValue(getEffectValue(grade))
                    .build());
        }
    }

    /** 효과 풀 조회 (장비 타입별) - public for EnhanceService */
    public List<EquipmentEffect> getEffectPool(EquipmentType type) {
        return switch (type) {
            case WEAPON -> WEAPON_EFFECTS;
            case HELMET, ARMOR, GLOVES, SHOES -> ARMOR_EFFECTS;
            case EARRING, RING -> ACCESSORY_EFFECTS;
        };
    }

    /** 등급별 효과 발동 확률 (%) */
    private int getEffectChance(EquipmentGrade grade) {
        return switch (grade) {
            case COMMON -> 5;
            case UNCOMMON -> 10;
            case RARE -> 15;
            case EPIC -> 20;
            case LEGENDARY -> 30;
        };
    }

    /** 등급별 효과 수치 */
    private int getEffectValue(EquipmentGrade grade) {
        return switch (grade) {
            case COMMON -> 1;
            case UNCOMMON -> 2;
            case RARE -> 3;
            case EPIC -> 4;
            case LEGENDARY -> 6;
        };
    }

    /** 장비 스탯 보너스 추가 */
    private void addStatBonuses(Equipment equipment, EquipmentGrade grade, EquipmentType type, ThreadLocalRandom random) {
        // 장갑/신발은 커먼도 보너스 스탯 부여
        boolean forceStats = (type == EquipmentType.GLOVES || type == EquipmentType.SHOES);
        if (!forceStats && grade.ordinal() < EquipmentGrade.UNCOMMON.ordinal()) return;

        int count = switch (grade) {
            case COMMON -> forceStats ? 1 : 0;
            case UNCOMMON -> 1;
            case RARE -> 1 + random.nextInt(2);   // 1-2
            case EPIC -> 2 + random.nextInt(2);    // 2-3
            case LEGENDARY -> 3 + random.nextInt(2); // 3-4
        };
        int maxValue = switch (grade) {
            case COMMON -> 1;
            case UNCOMMON -> 1;
            case RARE -> 2;
            case EPIC -> 3;
            case LEGENDARY -> 4;
        };

        List<String> pool = getStatPool(type);
        java.util.Set<String> used = new java.util.HashSet<>();

        for (int i = 0; i < count && used.size() < pool.size(); i++) {
            String stat;
            do { stat = pool.get(random.nextInt(pool.size())); } while (used.contains(stat));
            used.add(stat);
            int value = 1 + random.nextInt(maxValue);
            applyStat(equipment, stat, value);
        }
    }

    private List<String> getStatPool(EquipmentType type) {
        return switch (type) {
            case WEAPON -> List.of("STR", "DEX", "INT");
            case HELMET -> List.of("WIS", "INT", "CON");
            case ARMOR -> List.of("CON", "STR", "CHA");
            case GLOVES -> List.of("STR", "DEX");
            case SHOES -> List.of("DEX", "CON");
            case EARRING -> List.of("INT", "WIS", "CHA");
            case RING -> List.of("STR", "DEX", "CON", "INT", "WIS", "CHA");
        };
    }

    private void applyStat(Equipment eq, String stat, int value) {
        switch (stat) {
            case "STR" -> eq.setBonusStrength(eq.getBonusStrength() + value);
            case "DEX" -> eq.setBonusDexterity(eq.getBonusDexterity() + value);
            case "CON" -> eq.setBonusConstitution(eq.getBonusConstitution() + value);
            case "INT" -> eq.setBonusIntelligence(eq.getBonusIntelligence() + value);
            case "WIS" -> eq.setBonusWisdom(eq.getBonusWisdom() + value);
            case "CHA" -> eq.setBonusCharisma(eq.getBonusCharisma() + value);
        }
    }

    private String getGradePrefix(EquipmentGrade grade) {
        return switch (grade) {
            case COMMON -> "평범한";
            case UNCOMMON -> "견고한";
            case RARE -> "희귀한";
            case EPIC -> "영웅의";
            case LEGENDARY -> "전설의";
        };
    }
}
