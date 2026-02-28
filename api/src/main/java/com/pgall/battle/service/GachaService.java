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

import java.util.NoSuchElementException;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class GachaService {

    private static final int GACHA_COST = 30;

    private final GameCharacterRepository characterRepository;
    private final EquipmentRepository equipmentRepository;

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

        // RARE 이상 특수효과 부여 (60% 확률)
        if (grade.ordinal() >= EquipmentGrade.RARE.ordinal()) {
            EquipmentEffect effect = rollEffect(type);
            if (effect != null) {
                builder.effect(effect)
                        .effectChance(10 + gradeMultiplier * 5)
                        .effectValue(gradeMultiplier);
            }
        }

        return builder.build();
    }

    private EquipmentEffect rollEffect(EquipmentType type) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        if (random.nextInt(100) >= 60) return null;

        return switch (type) {
            case WEAPON -> random.nextBoolean() ? EquipmentEffect.DOUBLE_ATTACK : EquipmentEffect.LIFE_STEAL;
            case HELMET -> EquipmentEffect.STUN;
            case ARMOR -> EquipmentEffect.BLOCK_CHANCE;
            case GLOVES -> EquipmentEffect.ACCURACY_UP;
            case SHOES -> EquipmentEffect.DEBUFF_DEF_DOWN;
            case EARRING -> EquipmentEffect.DEBUFF_ATK_DOWN;
            case RING -> EquipmentEffect.POISON;
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
