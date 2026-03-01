package com.pgall.battle.dto;

import com.pgall.battle.entity.GameCharacter;
import lombok.*;
import java.util.List;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CharacterResponse {
    private Long id;
    private String name;
    private String avatar;
    private String characterClass;
    private String classKoreanName;
    private int strength;
    private int dexterity;
    private int constitution;
    private int intelligence;
    private int wisdom;
    private int charisma;
    private int level;
    private int hp;
    private int maxHp;
    private int gold;
    private int eloRate;
    private List<EquipmentResponse> equipments;
    private List<InventoryResponse> potions;

    public static CharacterResponse from(GameCharacter c) {
        return CharacterResponse.builder()
                .id(c.getId())
                .name(c.getName())
                .avatar(c.getAvatar())
                .characterClass(c.getCharacterClass() != null ? c.getCharacterClass().name() : null)
                .classKoreanName(c.getCharacterClass() != null ? c.getCharacterClass().getKoreanName() : null)
                .strength(c.getStrength())
                .dexterity(c.getDexterity())
                .constitution(c.getConstitution())
                .intelligence(c.getIntelligence())
                .wisdom(c.getWisdom())
                .charisma(c.getCharisma())
                .level(c.getLevel())
                .hp(c.getHp())
                .maxHp(c.getMaxHp())
                .gold(c.getGold())
                .eloRate(c.getEloRate())
                .equipments(c.getEquipments().stream().map(EquipmentResponse::from).toList())
                .potions(c.getInventories().stream()
                        .filter(inv -> inv.getQuantity() > 0 && inv.getShopItem() != null)
                        .map(InventoryResponse::from).toList())
                .build();
    }
}
