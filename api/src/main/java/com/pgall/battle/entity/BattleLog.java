package com.pgall.battle.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "battle_log")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BattleLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long attackerId;

    @Column(nullable = false)
    private Long defenderId;

    private Long winnerId;

    @Column
    private String log;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
