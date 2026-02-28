package com.pgall.battle.dto;

import com.pgall.battle.enums.BuffType;
import lombok.*;

import java.util.List;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShopResponse {
    private List<PotionItem> items;
    private int refreshCost;
    private int refreshCount;
    private int gold;

    @Getter @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PotionItem {
        private int index;
        private String name;
        private String description;
        private int price;
        private List<String> effects;
        private boolean sold;
    }
}
