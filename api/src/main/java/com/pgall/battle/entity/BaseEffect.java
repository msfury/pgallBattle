package com.pgall.battle.entity;

import com.pgall.battle.enums.EquipmentEffect;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "base_effect")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BaseEffect {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipment_id", nullable = false)
    private Equipment equipment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EquipmentEffect effect;

    private int effectChance;
    private int effectValue;
}
