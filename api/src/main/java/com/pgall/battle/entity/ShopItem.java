package com.pgall.battle.entity;

import com.pgall.battle.enums.BuffType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "shop_item")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShopItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;

    private int price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BuffType buffType;

    private int buffChance;
}
