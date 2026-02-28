package com.pgall.battle.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum WeaponCategory {
    // Two-handed weapons
    STAFF("지팡이", ScalingStat.INT, true, 1, 8, true),
    SPEAR("창", ScalingStat.STR, true, 1, 8, false),
    GREATSWORD("양손검", ScalingStat.STR, true, 2, 6, false),
    BOW("활", ScalingStat.DEX, true, 1, 8, false),

    // One-handed weapons
    SWORD("검", ScalingStat.STR, false, 1, 6, false),
    DAGGER("단검", ScalingStat.DEX, false, 1, 4, false),
    CLAW("클로", ScalingStat.DEX, false, 1, 4, false),
    MACE("철퇴", ScalingStat.STR, false, 1, 6, false),
    AXE("도끼", ScalingStat.STR, false, 1, 6, false),
    RAPIER("레이피어", ScalingStat.DEX, false, 1, 6, false),
    WAND("완드", ScalingStat.INT, false, 1, 6, true),
    FLAIL("플레일", ScalingStat.STR, false, 1, 6, false);

    private final String koreanName;
    private final ScalingStat scalingStat;
    private final boolean twoHanded;
    private final int diceCount;
    private final int diceSides;
    private final boolean magical;
}
