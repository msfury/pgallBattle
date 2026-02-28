package com.pgall.battle.entity;

import com.pgall.battle.enums.*;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "equipment")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Equipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EquipmentType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EquipmentGrade grade;

    private int attackBonus;
    private int defenseBonus;

    @Enumerated(EnumType.STRING)
    private EquipmentEffect effect;

    @Builder.Default
    private int effectChance = 0;

    @Builder.Default
    private int effectValue = 0;

    @Enumerated(EnumType.STRING)
    private WeaponCategory weaponCategory;

    private int baseDamageMin;
    private int baseDamageMax;

    @Enumerated(EnumType.STRING)
    private ScalingStat scalingStat;

    @Builder.Default
    private boolean twoHanded = false;

    @Builder.Default
    private boolean equipped = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "character_id")
    private GameCharacter character;
}
