package com.pgall.battle.service;

import com.pgall.battle.data.WeaponNameData;
import com.pgall.battle.dto.EquipmentResponse;
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
                WeaponCategory category = rollWeaponCategory();
                String baseName = WeaponNameData.getRandomName(category, random);
                name = getGradePrefix(grade) + " " + baseName;

                int dmgMin = category.getDiceCount() + (gradeMultiplier - 1);
                int dmgMax = category.getDiceCount() * category.getDiceSides() + gradeMultiplier;

                atkBonus = gradeMultiplier * random.nextInt(1, 4);

                builder.weaponCategory(category)
                       .scalingStat(category.getScalingStat())
                       .twoHanded(category.isTwoHanded())
                       .baseDamageMin(dmgMin)
                       .baseDamageMax(dmgMax);
            }
            case HELMET -> {
                defBonus = gradeMultiplier * random.nextInt(1, 3);
                name = getGradePrefix(grade) + " 투구";
            }
            case ARMOR -> {
                defBonus = gradeMultiplier * random.nextInt(2, 4);
                name = getGradePrefix(grade) + " 갑옷";
            }
            case GLOVES -> {
                defBonus = gradeMultiplier * random.nextInt(1, 3);
                atkBonus = gradeMultiplier;
                name = getGradePrefix(grade) + " 장갑";
            }
            case SHOES -> {
                defBonus = gradeMultiplier;
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

        // 효과 부여 - 등급별 확률
        EquipmentEffect effect = rollEffect(type, grade);
        if (effect != null) {
            builder.effect(effect)
                    .effectChance(getEffectChance(grade))
                    .effectValue(getEffectValue(grade));
        }

        return builder.build();
    }

    private EquipmentEffect rollEffect(EquipmentType type, EquipmentGrade grade) {
        ThreadLocalRandom random = ThreadLocalRandom.current();

        // 등급별 효과 획득 확률
        int chance = switch (grade) {
            case COMMON, UNCOMMON -> 40;
            case RARE -> 60;
            case EPIC -> 80;
            case LEGENDARY -> 100;
        };
        if (random.nextInt(100) >= chance) return null;

        // 슬롯별 효과 풀에서 랜덤 선택
        List<EquipmentEffect> pool = getEffectPool(type);
        return pool.get(random.nextInt(pool.size()));
    }

    private List<EquipmentEffect> getEffectPool(EquipmentType type) {
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
