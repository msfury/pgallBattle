package com.pgall.battle.controller;

import com.pgall.battle.dto.EquipmentResponse;
import com.pgall.battle.service.GachaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/gacha")
@RequiredArgsConstructor
public class GachaController {

    private final GachaService gachaService;

    @PostMapping("/{characterId}")
    public ResponseEntity<EquipmentResponse> pull(@PathVariable Long characterId) {
        return ResponseEntity.ok(gachaService.pull(characterId));
    }
}
