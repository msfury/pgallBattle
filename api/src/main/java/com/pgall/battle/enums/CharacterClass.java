package com.pgall.battle.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CharacterClass {
    WARRIOR("전사", ScalingStat.STR, 12),
    ROGUE("도적", ScalingStat.DEX, 8),
    MAGE("마법사", ScalingStat.INT, 6),
    CLERIC("성직자", ScalingStat.WIS, 8),
    RANGER("궁수", ScalingStat.DEX, 10);

    private final String koreanName;
    private final ScalingStat primaryStat;
    private final int hitDie;
}
