package com.pgall.battle.service;

import com.pgall.battle.dto.EquipmentResponse;
import com.pgall.battle.entity.Equipment;
import com.pgall.battle.entity.GameCharacter;
import com.pgall.battle.enums.EquipmentType;
import com.pgall.battle.repository.EquipmentRepository;
import com.pgall.battle.repository.GameCharacterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class EquipService {

    private final EquipmentRepository equipmentRepository;
    private final GameCharacterRepository characterRepository;

    @Transactional
    public EquipmentResponse equip(Long characterId, Long equipmentId) {
        GameCharacter character = characterRepository.findById(characterId)
                .orElseThrow(() -> new NoSuchElementException("캐릭터를 찾을 수 없습니다."));
        Equipment equipment = equipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new NoSuchElementException("장비를 찾을 수 없습니다."));

        if (!equipment.getCharacter().getId().equals(characterId)) {
            throw new IllegalArgumentException("이 캐릭터의 장비가 아닙니다.");
        }

        if (equipment.isEquipped()) {
            throw new IllegalStateException("이미 장착 중입니다.");
        }

        EquipmentType type = equipment.getType();
        long equippedCount = character.getEquipments().stream()
                .filter(e -> e.getType() == type && e.isEquipped())
                .count();

        if (equippedCount >= type.getMaxSlots()) {
            throw new IllegalStateException(type.name() + " 슬롯이 가득 찼습니다. ("
                    + equippedCount + "/" + type.getMaxSlots() + ")");
        }

        // 양손무기 체크
        if (type == EquipmentType.WEAPON) {
            boolean hasEquippedWeapon = character.getEquipments().stream()
                    .anyMatch(e -> e.getType() == EquipmentType.WEAPON && e.isEquipped());

            if (equipment.isTwoHanded() && hasEquippedWeapon) {
                throw new IllegalStateException("양손 무기는 다른 무기와 함께 장착할 수 없습니다.");
            }

            if (!equipment.isTwoHanded() && hasEquippedWeapon) {
                boolean hasTwoHanded = character.getEquipments().stream()
                        .anyMatch(e -> e.getType() == EquipmentType.WEAPON && e.isEquipped() && e.isTwoHanded());
                if (hasTwoHanded) {
                    throw new IllegalStateException("양손 무기가 장착되어 있어 다른 무기를 장착할 수 없습니다.");
                }
            }
        }

        equipment.setEquipped(true);
        equipmentRepository.save(equipment);
        return EquipmentResponse.from(equipment);
    }

    @Transactional
    public int sell(Long characterId, Long equipmentId) {
        GameCharacter character = characterRepository.findById(characterId)
                .orElseThrow(() -> new NoSuchElementException("캐릭터를 찾을 수 없습니다."));
        Equipment equipment = equipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new NoSuchElementException("장비를 찾을 수 없습니다."));

        if (!equipment.getCharacter().getId().equals(characterId)) {
            throw new IllegalArgumentException("이 캐릭터의 장비가 아닙니다.");
        }

        if (equipment.isEquipped()) {
            throw new IllegalStateException("장착 중인 장비는 판매할 수 없습니다. 먼저 해제하세요.");
        }

        int price = equipment.getGrade().getSellPrice();
        character.setGold(character.getGold() + price);
        character.getEquipments().remove(equipment);
        equipmentRepository.delete(equipment);
        characterRepository.save(character);
        return price;
    }

    @Transactional
    public EquipmentResponse unequip(Long characterId, Long equipmentId) {
        Equipment equipment = equipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new NoSuchElementException("장비를 찾을 수 없습니다."));

        if (!equipment.getCharacter().getId().equals(characterId)) {
            throw new IllegalArgumentException("이 캐릭터의 장비가 아닙니다.");
        }

        if (!equipment.isEquipped()) {
            throw new IllegalStateException("장착되어 있지 않습니다.");
        }

        equipment.setEquipped(false);
        equipmentRepository.save(equipment);
        return EquipmentResponse.from(equipment);
    }
}
