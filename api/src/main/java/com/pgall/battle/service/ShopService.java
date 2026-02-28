package com.pgall.battle.service;

import com.pgall.battle.dto.ShopBuyRequest;
import com.pgall.battle.entity.GameCharacter;
import com.pgall.battle.entity.Inventory;
import com.pgall.battle.entity.ShopItem;
import com.pgall.battle.repository.GameCharacterRepository;
import com.pgall.battle.repository.InventoryRepository;
import com.pgall.battle.repository.ShopItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class ShopService {

    private final ShopItemRepository shopItemRepository;
    private final GameCharacterRepository characterRepository;
    private final InventoryRepository inventoryRepository;

    public List<ShopItem> getAllItems() {
        return shopItemRepository.findAll();
    }

    @Transactional
    public Inventory buyItem(ShopBuyRequest request) {
        GameCharacter character = characterRepository.findById(request.getCharacterId())
                .orElseThrow(() -> new NoSuchElementException("캐릭터를 찾을 수 없습니다."));
        ShopItem item = shopItemRepository.findById(request.getShopItemId())
                .orElseThrow(() -> new NoSuchElementException("아이템을 찾을 수 없습니다."));

        if (character.getGold() < item.getPrice()) {
            throw new IllegalStateException("골드가 부족합니다. (필요: " + item.getPrice() + ", 보유: " + character.getGold() + ")");
        }

        character.setGold(character.getGold() - item.getPrice());
        characterRepository.save(character);

        Inventory inventory = inventoryRepository
                .findByCharacterIdAndShopItemId(request.getCharacterId(), request.getShopItemId())
                .map(inv -> {
                    inv.setQuantity(inv.getQuantity() + 1);
                    return inv;
                })
                .orElse(Inventory.builder()
                        .character(character)
                        .shopItem(item)
                        .quantity(1)
                        .build());

        return inventoryRepository.save(inventory);
    }
}
