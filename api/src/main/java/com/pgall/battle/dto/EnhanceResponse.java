package com.pgall.battle.dto;

import com.pgall.battle.enums.EquipmentEffect;
import lombok.*;

import java.util.List;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnhanceResponse {
    private boolean success;
    private boolean broken;
    private int newLevel;
    private int cost;
    private String message;
    // 강화 정보용
    private int successRate;
    private int breakChance;
    private int nextStatBonus;
    // 효과 선택이 필요한 경우
    private boolean needsEffectSelection;
    private int maxEnhanceEffects;
    private List<EffectOption> currentEnhanceEffects;
    private List<EffectOption> candidateEffects;

    @Getter @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EffectOption {
        private int index;
        private EquipmentEffect effect;
        private String effectName;
        private int effectChance;
        private int effectValue;
    }
}
