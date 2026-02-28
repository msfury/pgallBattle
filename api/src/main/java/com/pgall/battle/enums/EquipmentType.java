package com.pgall.battle.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum EquipmentType {
    WEAPON(1),
    HELMET(1),
    ARMOR(1),
    GLOVES(1),
    SHOES(1),
    EARRING(2),
    RING(2);

    private final int maxSlots;
}
