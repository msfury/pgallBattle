package com.pgall.battle.entity;

import com.pgall.battle.enums.CharacterClass;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "game_character")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GameCharacter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String avatar;

    @Enumerated(EnumType.STRING)
    private CharacterClass characterClass;

    private int strength;
    private int dexterity;
    private int constitution;
    private int intelligence;
    private int wisdom;
    private int charisma;

    @Builder.Default
    private int level = 1;

    private int hp;
    private int maxHp;

    @Builder.Default
    private int gold = 100;

    @Builder.Default
    private int eloRate = 1000;

    private String ipAddress;

    private LocalDate lastDailyGoldDate;

    @OneToMany(mappedBy = "character", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Equipment> equipments = new ArrayList<>();

    @OneToMany(mappedBy = "character", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Inventory> inventories = new ArrayList<>();

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (maxHp == 0) {
            int baseHp = characterClass != null ? characterClass.getHitDie() : 10;
            maxHp = baseHp + getModifier(constitution);
            if (maxHp < 1) maxHp = 1;
            hp = maxHp;
        }
    }

    public static int getModifier(int stat) {
        return (stat - 10) / 2;
    }
}
