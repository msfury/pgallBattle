package com.pgall.battle.dto;

import lombok.*;

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
}
