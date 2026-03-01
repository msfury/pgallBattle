package com.pgall.battle.entity;

import com.pgall.battle.enums.*;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

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

    @Builder.Default
    @Column(columnDefinition = "integer default 0")
    private int enhanceLevel = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "character_id")
    private GameCharacter character;

    @OneToMany(mappedBy = "equipment", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<BaseEffect> baseEffects = new ArrayList<>();

    @OneToMany(mappedBy = "equipment", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<EnhanceEffect> enhanceEffects = new ArrayList<>();

    @Builder.Default
    @Column(columnDefinition = "integer default 0")
    private int enhanceEffectSlots = 0;

    // 장비 스탯 보너스 (언커먼 이상)
    @Builder.Default
    @Column(columnDefinition = "integer default 0")
    private int bonusStrength = 0;

    @Builder.Default
    @Column(columnDefinition = "integer default 0")
    private int bonusDexterity = 0;

    @Builder.Default
    @Column(columnDefinition = "integer default 0")
    private int bonusConstitution = 0;

    @Builder.Default
    @Column(columnDefinition = "integer default 0")
    private int bonusIntelligence = 0;

    @Builder.Default
    @Column(columnDefinition = "integer default 0")
    private int bonusWisdom = 0;

    @Builder.Default
    @Column(columnDefinition = "integer default 0")
    private int bonusCharisma = 0;
}
