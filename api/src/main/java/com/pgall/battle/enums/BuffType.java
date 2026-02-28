package com.pgall.battle.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BuffType {
    // 치유 (HP 위험 시 자동 사용)
    HEAL("치유 물약", true),
    GREATER_HEAL("고급 치유 물약", true),

    // 버프 (공격 1회 사용)
    CRIT_DOUBLE("크리티컬 강화", false),
    DOUBLE_ATTACK("더블 어택", false),
    SHIELD("수호의 방패", false),
    FIRE_ENCHANT("화염 부여", false),
    ICE_ENCHANT("빙결 부여", false),
    LIGHTNING_ENCHANT("번개 부여", false),
    HOLY_ENCHANT("신성 부여", false),
    PENETRATION_BOOST("관통 강화", false),
    REGEN_POTION("재생 물약", false),
    REFLECT_POTION("반사 물약", false),
    ACCURACY_POTION("정확도 물약", false),
    HASTE_POTION("가속 물약", false),
    IRON_SKIN_POTION("철피 물약", false),
    BLESS_POTION("축복 물약", false);

    private final String koreanName;
    private final boolean healType;
}
