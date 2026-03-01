package com.pgall.battle.dto;

import com.pgall.battle.entity.Equipment;
import com.pgall.battle.enums.EquipmentEffect;
import com.pgall.battle.enums.EquipmentGrade;
import com.pgall.battle.enums.EquipmentType;
import lombok.*;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EquipmentResponse {
    private Long id;
    private String name;
    private EquipmentType type;
    private EquipmentGrade grade;
    private int attackBonus;
    private int defenseBonus;
    private EquipmentEffect effect;
    private int effectChance;
    private int effectValue;
    private boolean equipped;
    private String weaponCategory;
    private String scalingStat;
    private boolean twoHanded;
    private int baseDamageMin;
    private int baseDamageMax;
    private int enhanceLevel;

    public static EquipmentResponse from(Equipment e) {
        return EquipmentResponse.builder()
                .id(e.getId())
                .name(e.getName())
                .type(e.getType())
                .grade(e.getGrade())
                .attackBonus(e.getAttackBonus())
                .defenseBonus(e.getDefenseBonus())
                .effect(e.getEffect())
                .effectChance(e.getEffectChance())
                .effectValue(e.getEffectValue())
                .equipped(e.isEquipped())
                .weaponCategory(e.getWeaponCategory() != null ? e.getWeaponCategory().name() : null)
                .scalingStat(e.getScalingStat() != null ? e.getScalingStat().name() : null)
                .twoHanded(e.isTwoHanded())
                .baseDamageMin(e.getBaseDamageMin())
                .baseDamageMax(e.getBaseDamageMax())
                .enhanceLevel(e.getEnhanceLevel())
                .build();
    }
}
