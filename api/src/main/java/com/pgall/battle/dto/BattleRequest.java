package com.pgall.battle.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class BattleRequest {
    private Long attackerId;
    private Long defenderId;
}
