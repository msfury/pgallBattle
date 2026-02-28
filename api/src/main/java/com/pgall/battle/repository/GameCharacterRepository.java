package com.pgall.battle.repository;

import com.pgall.battle.entity.GameCharacter;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameCharacterRepository extends JpaRepository<GameCharacter, Long> {
    java.util.List<GameCharacter> findAllByOrderByEloRateDesc();
}
