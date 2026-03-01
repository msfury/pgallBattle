package com.pgall.battle.service;

import com.pgall.battle.dto.EnhanceResponse;
import com.pgall.battle.entity.EnhanceEffect;
import com.pgall.battle.entity.Equipment;
import com.pgall.battle.entity.GameCharacter;
import com.pgall.battle.enums.EquipmentEffect;
import com.pgall.battle.enums.EquipmentType;
import com.pgall.battle.repository.EquipmentRepository;
import com.pgall.battle.repository.GameCharacterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class EnhanceService {

    private final EquipmentRepository equipmentRepository;
    private final GameCharacterRepository characterRepository;

    private static final List<EquipmentEffect> WEAPON_EFFECTS =
            Arrays.stream(EquipmentEffect.values())
                    .filter(e -> e.getCategory() == EquipmentEffect.Category.WEAPON).toList();

    @Transactional
    public EnhanceResponse enhance(Long characterId, Long equipmentId) {
        GameCharacter character = characterRepository.findById(characterId)
                .orElseThrow(() -> new NoSuchElementException("캐릭터를 찾을 수 없습니다."));
        Equipment eq = equipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new NoSuchElementException("장비를 찾을 수 없습니다."));

        if (!eq.getCharacter().getId().equals(characterId)) {
            throw new IllegalArgumentException("이 캐릭터의 장비가 아닙니다.");
        }
        if (eq.getType() != EquipmentType.WEAPON) {
            throw new IllegalStateException("무기만 강화할 수 있습니다.");
        }

        int currentLevel = eq.getEnhanceLevel();
        int cost = getEnhanceCost(currentLevel);

        if (character.getGold() < cost) {
            throw new IllegalStateException("골드 부족 (필요: " + cost + "G, 보유: " + character.getGold() + "G)");
        }

        // 골드 차감
        character.setGold(character.getGold() - cost);
        characterRepository.save(character);

        ThreadLocalRandom random = ThreadLocalRandom.current();
        int successRate = getSuccessRate(currentLevel);
        int breakChance = getBreakChance(currentLevel);

        // 성공 판정
        int roll = random.nextInt(100);
        if (roll < successRate) {
            // 성공!
            int newLevel = currentLevel + 1;
            int oldBonus = getStatBonus(currentLevel);
            int newBonus = getStatBonus(newLevel);
            int delta = newBonus - oldBonus;

            if (delta > 0) {
                eq.setAttackBonus(eq.getAttackBonus() + delta);
                eq.setBaseDamageMin(eq.getBaseDamageMin() + delta);
                eq.setBaseDamageMax(eq.getBaseDamageMax() + delta);
            }

            // 효과 추가 (마일스톤 달성 시)
            int oldEffects = getEffectCount(currentLevel);
            int newEffects = getEffectCount(newLevel);
            int effectDelta = newEffects - oldEffects;
            if (effectDelta > 0) {
                addRandomEffects(eq, effectDelta, random);
            }

            eq.setEnhanceLevel(newLevel);
            // 이름에 강화 표시 업데이트
            eq.setName(updateEnhanceName(eq.getName(), newLevel));
            equipmentRepository.save(eq);

            return EnhanceResponse.builder()
                    .success(true).broken(false)
                    .newLevel(newLevel).cost(cost)
                    .message("강화 성공! +" + newLevel)
                    .build();
        } else {
            // 실패 - 깨짐 판정
            int breakRoll = random.nextInt(100);
            if (breakRoll < breakChance) {
                // 무기 파괴!
                character.getEquipments().remove(eq);
                equipmentRepository.delete(eq);

                return EnhanceResponse.builder()
                        .success(false).broken(true)
                        .newLevel(0).cost(cost)
                        .message("강화 실패! 무기가 파괴되었습니다!")
                        .build();
            } else {
                return EnhanceResponse.builder()
                        .success(false).broken(false)
                        .newLevel(currentLevel).cost(cost)
                        .message("강화 실패! (+" + currentLevel + " 유지)")
                        .build();
            }
        }
    }

    /** 강화 정보 조회 */
    public EnhanceResponse getInfo(Long equipmentId) {
        Equipment eq = equipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new NoSuchElementException("장비를 찾을 수 없습니다."));

        int level = eq.getEnhanceLevel();
        return EnhanceResponse.builder()
                .success(false).broken(false)
                .newLevel(level)
                .cost(getEnhanceCost(level))
                .successRate(getSuccessRate(level))
                .breakChance(getBreakChance(level))
                .nextStatBonus(getStatBonus(level + 1) - getStatBonus(level))
                .build();
    }

    // ===== 강화 계산 =====

    /** 강화 비용 */
    int getEnhanceCost(int currentLevel) {
        if (currentLevel < 3) return 5;
        if (currentLevel < 6) return 10;
        if (currentLevel < 9) return 25;
        return 100;
    }

    /** 성공 확률 (%) */
    int getSuccessRate(int currentLevel) {
        if (currentLevel < 3) return 80;
        if (currentLevel < 6) return 60;
        if (currentLevel < 9) return 50;
        return 30;
    }

    /** 파괴 확률 (%) - 실패 시에만 적용 */
    int getBreakChance(int currentLevel) {
        if (currentLevel < 3) return 0;
        if (currentLevel < 6) return 5;
        if (currentLevel < 9) return 10;
        return 15;
    }

    /**
     * 피보나치형 스탯 보너스 (마일스톤 +3 단위)
     * +3: 1, +6: 3, +9: 5, +12: 8, +15: 13, +18: 21, ...
     */
    int getStatBonus(int enhanceLevel) {
        int milestone = enhanceLevel / 3;
        if (milestone <= 0) return 0;
        if (milestone == 1) return 1;
        if (milestone == 2) return 3;
        if (milestone == 3) return 5;
        int prev2 = 3, prev1 = 5;
        for (int i = 4; i <= milestone; i++) {
            int next = prev1 + prev2;
            prev2 = prev1;
            prev1 = next;
        }
        return prev1;
    }

    /**
     * 장비 효과 개수 (마일스톤 +6부터)
     * +6: 1, +9: 3, +12: 5, +15: 7, ...
     */
    int getEffectCount(int enhanceLevel) {
        int milestone = enhanceLevel / 3;
        if (milestone < 2) return 0;
        return 2 * (milestone - 1) - 1;
    }

    private void addRandomEffects(Equipment eq, int count, ThreadLocalRandom random) {
        for (int i = 0; i < count; i++) {
            EquipmentEffect effect = WEAPON_EFFECTS.get(random.nextInt(WEAPON_EFFECTS.size()));
            int chance = 10 + random.nextInt(20); // 10~29%
            int value = 2 + random.nextInt(4); // 2~5
            EnhanceEffect ee = EnhanceEffect.builder()
                    .equipment(eq).effect(effect)
                    .effectChance(chance).effectValue(value)
                    .build();
            eq.getEnhanceEffects().add(ee);
        }
    }

    private String updateEnhanceName(String name, int level) {
        // 기존 강화 표시 제거
        String base = name.replaceAll("\\s*\\+\\d+$", "");
        return base + " +" + level;
    }
}
