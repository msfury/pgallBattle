package com.pgall.battle.dto;

import com.pgall.battle.entity.Inventory;
import lombok.*;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryResponse {
    private Long id;
    private String name;
    private String description;
    private String buffType;
    private int quantity;
    private boolean equipped;
    private int buyPrice;
    private int sellPrice;

    public static InventoryResponse from(Inventory inv) {
        var item = inv.getShopItem();
        return InventoryResponse.builder()
                .id(inv.getId())
                .name(item != null ? item.getName() : "알 수 없음")
                .description(item != null ? item.getDescription() : "")
                .buffType(item != null && item.getBuffType() != null ? item.getBuffType().name() : null)
                .quantity(inv.getQuantity())
                .equipped(inv.isEquipped())
                .buyPrice(item != null ? item.getPrice() : 0)
                .sellPrice(item != null ? item.getPrice() / 2 : 0)
                .build();
    }
}
