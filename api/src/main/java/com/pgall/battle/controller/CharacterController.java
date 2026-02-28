package com.pgall.battle.controller;

import com.pgall.battle.dto.*;
import com.pgall.battle.service.CharacterService;
import com.pgall.battle.service.EquipService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/characters")
@RequiredArgsConstructor
public class CharacterController {

    private final CharacterService characterService;
    private final EquipService equipService;
    private final com.pgall.battle.repository.GameCharacterRepository characterRepository;

    @GetMapping("/random-stats")
    public ResponseEntity<RandomStatsResponse> randomStats() {
        return ResponseEntity.ok(characterService.generateRandomStats());
    }

    @PostMapping
    public ResponseEntity<CharacterResponse> create(@Valid @RequestBody CharacterCreateRequest request) {
        return ResponseEntity.ok(characterService.createCharacter(request));
    }

    @GetMapping
    public ResponseEntity<List<CharacterResponse>> list() {
        return ResponseEntity.ok(characterService.getAllCharacters());
    }

    @GetMapping("/ranking")
    public ResponseEntity<List<CharacterResponse>> ranking() {
        return ResponseEntity.ok(characterRepository.findAllByOrderByEloRateDesc()
                .stream().map(CharacterResponse::from).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CharacterResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(characterService.getCharacter(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        characterService.deleteCharacter(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{charId}/equipment/{equipId}/equip")
    public ResponseEntity<EquipmentResponse> equip(@PathVariable Long charId, @PathVariable Long equipId) {
        return ResponseEntity.ok(equipService.equip(charId, equipId));
    }

    @PutMapping("/{charId}/equipment/{equipId}/unequip")
    public ResponseEntity<EquipmentResponse> unequip(@PathVariable Long charId, @PathVariable Long equipId) {
        return ResponseEntity.ok(equipService.unequip(charId, equipId));
    }
}
