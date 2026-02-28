package com.pgall.battle.controller;

import com.pgall.battle.dto.BattleRequest;
import com.pgall.battle.dto.BattleResponse;
import com.pgall.battle.entity.BattleLog;
import com.pgall.battle.service.BattleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/battle")
@RequiredArgsConstructor
public class BattleController {

    private final BattleService battleService;

    @PostMapping
    public ResponseEntity<BattleResponse> battle(@RequestBody BattleRequest request) {
        return ResponseEntity.ok(battleService.battle(request));
    }

    @GetMapping("/logs/{characterId}")
    public ResponseEntity<List<BattleLog>> logs(@PathVariable Long characterId) {
        return ResponseEntity.ok(battleService.getLogs(characterId));
    }
}
