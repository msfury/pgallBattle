package com.pgall.battle.controller;

import com.pgall.battle.dto.ShopBuyRequest;
import com.pgall.battle.entity.Inventory;
import com.pgall.battle.entity.ShopItem;
import com.pgall.battle.service.ShopService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shop")
@RequiredArgsConstructor
public class ShopController {

    private final ShopService shopService;

    @GetMapping("/items")
    public ResponseEntity<List<ShopItem>> list() {
        return ResponseEntity.ok(shopService.getAllItems());
    }

    @PostMapping("/buy")
    public ResponseEntity<Inventory> buy(@RequestBody ShopBuyRequest request) {
        return ResponseEntity.ok(shopService.buyItem(request));
    }
}
