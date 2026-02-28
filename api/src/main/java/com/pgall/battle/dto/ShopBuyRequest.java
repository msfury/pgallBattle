package com.pgall.battle.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class ShopBuyRequest {
    private Long characterId;
    private Long shopItemId;
}
