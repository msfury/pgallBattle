package com.pgall.battle.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum EquipmentGrade {
    COMMON(5),
    UNCOMMON(10),
    RARE(20),
    EPIC(80),
    LEGENDARY(200);

    private final int sellPrice;
}
