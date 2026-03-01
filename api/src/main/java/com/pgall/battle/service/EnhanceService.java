package com.pgall.battle.service;

import com.pgall.battle.dto.EnhanceResponse;
import com.pgall.battle.entity.EnhanceEffect;
import com.pgall.battle.entity.Equipment;
import com.pgall.battle.entity.GameCharacter;
import com.pgall.battle.enums.EquipmentEffect;
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
    private final GachaService gachaService;

    @Transactional
    public EnhanceResponse enhance(Long characterId, Long equipmentId) {
        GameCharacter character = characterRepository.findById(characterId)
                .orElseThrow(() -> new NoSuchElementException("캐릭터를 찾을 수 없습니다."));
        Equipment eq = equipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new NoSuchElementException("장비를 찾을 수 없습니다."));

        if (!eq.getCharacter().getId().equals(characterId)) {
            throw new IllegalArgumentException("이 캐릭터의 장비가 아닙니다.");
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
                if (eq.getBaseDamageMax() > 0) {
                    // 무기: ATK + 데미지 증가
                    eq.setAttackBonus(eq.getAttackBonus() + delta);
                    eq.setBaseDamageMin(eq.getBaseDamageMin() + delta);
                    eq.setBaseDamageMax(eq.getBaseDamageMax() + delta);
                } else {
                    // 비무기: DEF만 증가
                    eq.setDefenseBonus(eq.getDefenseBonus() + delta);
                }
            }

            eq.setEnhanceLevel(newLevel);
            eq.setName(updateEnhanceName(eq.getName(), newLevel));

            // 강화 효과 슬롯 체크 (+4부터 +3간격)
            int newSlots = getEnhanceEffectSlots(newLevel);
            eq.setEnhanceEffectSlots(newSlots);

            EnhanceResponse.EnhanceResponseBuilder builder = EnhanceResponse.builder()
                    .success(true).broken(false)
                    .newLevel(newLevel).cost(cost)
                    .message("강화 성공! +" + newLevel);

            // +4 이상: 매 강화 시 새 효과 생성
            if (newLevel >= 4 && newSlots > 0) {
                List<EnhanceResponse.EffectOption> candidates = generateCandidateEffects(eq, 1, random);
                int currentCount = eq.getEnhanceEffects().size();

                if (currentCount < newSlots && !candidates.isEmpty()) {
                    // 빈 슬롯 있음 → 자동 추가
                    EnhanceResponse.EffectOption candidate = candidates.get(0);
                    eq.getEnhanceEffects().add(EnhanceEffect.builder()
                            .equipment(eq)
                            .effect(candidate.getEffect())
                            .effectChance(candidate.getEffectChance())
                            .effectValue(candidate.getEffectValue())
                            .build());
                    builder.message("강화 성공! +" + newLevel + " [" + candidate.getEffectName() + " 효과 추가]");
                } else if (!candidates.isEmpty()) {
                    // 슬롯 꽉 참 → 기존 + 후보 중 선택 필요
                    List<EnhanceResponse.EffectOption> current = getCurrentEnhanceEffects(eq);
                    builder.needsEffectSelection(true)
                            .maxEnhanceEffects(newSlots)
                            .currentEnhanceEffects(current)
                            .candidateEffects(candidates);
                }
            }

            equipmentRepository.save(eq);

            return builder.build();
        } else {
            // 실패 - 깨짐 판정
            int breakRoll = random.nextInt(100);
            if (breakRoll < breakChance) {
                // 장비 파괴!
                character.getEquipments().remove(eq);
                equipmentRepository.delete(eq);

                return EnhanceResponse.builder()
                        .success(false).broken(true)
                        .newLevel(0).cost(cost)
                        .message("강화 실패! 장비가 파괴되었습니다!")
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

    /** 효과 선택 확정 */
    @Transactional
    public void confirmEffects(Long characterId, Long equipmentId, List<String> selectedEffects) {
        Equipment eq = equipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new NoSuchElementException("장비를 찾을 수 없습니다."));

        if (!eq.getCharacter().getId().equals(characterId)) {
            throw new IllegalArgumentException("이 캐릭터의 장비가 아닙니다.");
        }

        int maxSlots = eq.getEnhanceEffectSlots();
        if (selectedEffects.size() > maxSlots) {
            throw new IllegalArgumentException("최대 " + maxSlots + "개까지 선택 가능합니다.");
        }

        ThreadLocalRandom random = ThreadLocalRandom.current();

        // 기존 강화 효과 전체 교체
        eq.getEnhanceEffects().clear();

        for (String effectName : selectedEffects) {
            EquipmentEffect effect = EquipmentEffect.valueOf(effectName);
            eq.getEnhanceEffects().add(EnhanceEffect.builder()
                    .equipment(eq)
                    .effect(effect)
                    .effectChance(10 + random.nextInt(20))
                    .effectValue(2 + random.nextInt(4))
                    .build());
        }

        equipmentRepository.save(eq);
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
                .maxEnhanceEffects(getEnhanceEffectSlots(level))
                .currentEnhanceEffects(getCurrentEnhanceEffects(eq))
                .build();
    }

    // ===== 강화 계산 =====

    int getEnhanceCost(int currentLevel) {
        if (currentLevel < 3) return 5;
        if (currentLevel < 6) return 10;
        if (currentLevel < 9) return 25;
        return 100;
    }

    int getSuccessRate(int currentLevel) {
        if (currentLevel < 3) return 80;
        if (currentLevel < 6) return 60;
        if (currentLevel < 9) return 50;
        return 30;
    }

    int getBreakChance(int currentLevel) {
        if (currentLevel < 3) return 0;
        if (currentLevel < 6) return 5;
        if (currentLevel < 9) return 10;
        return 15;
    }

    /**
     * 선형 스탯 보너스 (마일스톤 +3 단위)
     * +3: 1, +6: 2, +9: 3, +12: 4, +15: 5, +18: 6, ...
     */
    int getStatBonus(int enhanceLevel) {
        int milestone = enhanceLevel / 3;
        return milestone;
    }

    /**
     * 강화 효과 슬롯 수 (+4부터 시작, +3 간격)
     * +4: 1, +7: 2, +10: 3, +13: 4, ...
     */
    int getEnhanceEffectSlots(int enhanceLevel) {
        if (enhanceLevel < 4) return 0;
        return (enhanceLevel - 4) / 3 + 1;
    }

    /** 후보 효과 생성 (장비 타입에 맞는 풀에서) */
    private List<EnhanceResponse.EffectOption> generateCandidateEffects(Equipment eq, int count, ThreadLocalRandom random) {
        List<EquipmentEffect> pool = gachaService.getEffectPool(eq.getType());
        Set<EquipmentEffect> used = new HashSet<>();
        // 기존 강화 효과 제외
        eq.getEnhanceEffects().forEach(ee -> used.add(ee.getEffect()));

        List<EnhanceResponse.EffectOption> candidates = new ArrayList<>();
        for (int i = 0; i < count && used.size() < pool.size(); i++) {
            EquipmentEffect effect;
            do {
                effect = pool.get(random.nextInt(pool.size()));
            } while (used.contains(effect));
            used.add(effect);

            candidates.add(EnhanceResponse.EffectOption.builder()
                    .index(i)
                    .effect(effect)
                    .effectName(effect.getKoreanName())
                    .effectChance(10 + random.nextInt(20))
                    .effectValue(2 + random.nextInt(4))
                    .build());
        }
        return candidates;
    }

    /** 현재 강화 효과 목록 */
    private List<EnhanceResponse.EffectOption> getCurrentEnhanceEffects(Equipment eq) {
        List<EnhanceResponse.EffectOption> list = new ArrayList<>();
        int i = 0;
        for (var ee : eq.getEnhanceEffects()) {
            list.add(EnhanceResponse.EffectOption.builder()
                    .index(i++)
                    .effect(ee.getEffect())
                    .effectName(ee.getEffect().getKoreanName())
                    .effectChance(ee.getEffectChance())
                    .effectValue(ee.getEffectValue())
                    .build());
        }
        return list;
    }

    private String updateEnhanceName(String name, int level) {
        String base = name.replaceAll("\\s*\\+\\d+$", "");
        return base + " +" + level;
    }
}
