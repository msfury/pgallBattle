package com.pgall.battle.controller;

import com.pgall.battle.dto.ShopResponse;
import com.pgall.battle.service.ShopService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/shop")
@RequiredArgsConstructor
public class ShopController {

    private final ShopService shopService;

    @GetMapping("/{characterId}/items")
    public ResponseEntity<ShopResponse> getShop(@PathVariable Long characterId) {
        return ResponseEntity.ok(shopService.getShop(characterId));
    }

    @PostMapping("/{characterId}/refresh")
    public ResponseEntity<ShopResponse> refresh(@PathVariable Long characterId) {
        return ResponseEntity.ok(shopService.refresh(characterId));
    }

    @PostMapping("/{characterId}/buy/{index}")
    public ResponseEntity<ShopResponse> buy(@PathVariable Long characterId, @PathVariable int index) {
        return ResponseEntity.ok(shopService.buyItem(characterId, index));
    }
}
