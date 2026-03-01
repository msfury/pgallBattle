package com.pgall.battle.controller;

import com.pgall.battle.dto.*;
import com.pgall.battle.filter.IpOwnershipFilter;
import com.pgall.battle.service.CharacterService;
import com.pgall.battle.service.EnhanceService;
import com.pgall.battle.service.EquipService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/characters")
@RequiredArgsConstructor
public class CharacterController {

    private final CharacterService characterService;
    private final EquipService equipService;
    private final EnhanceService enhanceService;
    private final com.pgall.battle.repository.GameCharacterRepository characterRepository;

    @GetMapping("/random-stats")
    public ResponseEntity<RandomStatsResponse> randomStats() {
        return ResponseEntity.ok(characterService.generateRandomStats());
    }

    @PostMapping
    public ResponseEntity<CharacterResponse> create(@Valid @RequestBody CharacterCreateRequest request,
                                                     HttpServletRequest httpRequest) {
        String ip = IpOwnershipFilter.extractIp(httpRequest);
        return ResponseEntity.ok(characterService.createCharacter(request, ip));
    }

    /** 내 IP로 생성된 캐릭터 조회 */
    @GetMapping("/mine")
    public ResponseEntity<CharacterResponse> mine(HttpServletRequest httpRequest) {
        String ip = IpOwnershipFilter.extractIp(httpRequest);
        return ResponseEntity.ok(characterService.getMyCharacter(ip));
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

    @PostMapping("/{charId}/equipment/{equipId}/sell")
    public ResponseEntity<Map<String, Object>> sell(@PathVariable Long charId, @PathVariable Long equipId) {
        int price = equipService.sell(charId, equipId);
        return ResponseEntity.ok(Map.of("soldPrice", price));
    }

    @PostMapping("/{id}/daily-check")
    public ResponseEntity<Map<String, Object>> dailyCheck(@PathVariable Long id) {
        boolean granted = characterService.checkAndGrantDailyGold(id);
        return ResponseEntity.ok(Map.of("granted", granted, "amount", granted ? 300 : 0));
    }

    @PutMapping("/{charId}/equipment/{equipId}/equip")
    public ResponseEntity<EquipmentResponse> equip(@PathVariable Long charId, @PathVariable Long equipId) {
        return ResponseEntity.ok(equipService.equip(charId, equipId));
    }

    @PutMapping("/{charId}/equipment/{equipId}/unequip")
    public ResponseEntity<EquipmentResponse> unequip(@PathVariable Long charId, @PathVariable Long equipId) {
        return ResponseEntity.ok(equipService.unequip(charId, equipId));
    }

    // ===== 물약 장착/해제/판매 =====

    @PutMapping("/{charId}/potion/{invId}/equip")
    public ResponseEntity<Map<String, Object>> equipPotion(@PathVariable Long charId, @PathVariable Long invId) {
        equipService.equipPotion(charId, invId);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PutMapping("/{charId}/potion/{invId}/unequip")
    public ResponseEntity<Map<String, Object>> unequipPotion(@PathVariable Long charId, @PathVariable Long invId) {
        equipService.unequipPotion(charId, invId);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PostMapping("/{charId}/potion/{invId}/sell")
    public ResponseEntity<Map<String, Object>> sellPotion(@PathVariable Long charId, @PathVariable Long invId) {
        int price = equipService.sellPotion(charId, invId);
        return ResponseEntity.ok(Map.of("soldPrice", price));
    }

    // ===== 무기 강화 =====

    @PostMapping("/{charId}/equipment/{equipId}/enhance")
    public ResponseEntity<EnhanceResponse> enhance(@PathVariable Long charId, @PathVariable Long equipId) {
        return ResponseEntity.ok(enhanceService.enhance(charId, equipId));
    }

    @GetMapping("/{charId}/equipment/{equipId}/enhance-info")
    public ResponseEntity<EnhanceResponse> enhanceInfo(@PathVariable Long charId, @PathVariable Long equipId) {
        return ResponseEntity.ok(enhanceService.getInfo(equipId));
    }

    @PostMapping("/{charId}/equipment/{equipId}/enhance-effects")
    public ResponseEntity<Map<String, Object>> confirmEnhanceEffects(
            @PathVariable Long charId, @PathVariable Long equipId,
            @RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<String> selectedEffects = (List<String>) body.get("selectedEffects");
        enhanceService.confirmEffects(charId, equipId, selectedEffects);
        return ResponseEntity.ok(Map.of("success", true));
    }
}
