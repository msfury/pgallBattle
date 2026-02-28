package com.pgall.battle.repository;

import com.pgall.battle.entity.Equipment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EquipmentRepository extends JpaRepository<Equipment, Long> {
    List<Equipment> findByCharacterId(Long characterId);
    List<Equipment> findByCharacterIdAndEquipped(Long characterId, boolean equipped);
    void deleteByCharacterIdAndEquipped(Long characterId, boolean equipped);
}
