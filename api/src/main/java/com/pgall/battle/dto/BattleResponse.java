package com.pgall.battle.dto;

import lombok.*;
import java.util.List;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BattleResponse {
    private Long winnerId;
    private String winnerName;
    private Long loserId;
    private String loserName;
    private List<String> battleLog;
    private int goldReward;
    private String attackerName;
    private String defenderName;
    private String attackerAvatar;
    private String defenderAvatar;
    private String attackerClass;
    private String defenderClass;
    private int attackerMaxHp;
    private int defenderMaxHp;
    private List<PotionInfo> attackerPotions;
    private List<PotionInfo> defenderPotions;

    @Getter @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PotionInfo {
        private String name;
        private String buffType;
        private int quantity;
    }
}
