package com.pgall.battle.repository;

import com.pgall.battle.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    List<Inventory> findByCharacterId(Long characterId);
    List<Inventory> findByCharacterIdAndEquipped(Long characterId, boolean equipped);
    Optional<Inventory> findByCharacterIdAndShopItemId(Long characterId, Long shopItemId);
    long countByCharacterIdAndEquipped(Long characterId, boolean equipped);
}
