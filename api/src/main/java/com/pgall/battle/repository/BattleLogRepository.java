package com.pgall.battle.repository;

import com.pgall.battle.entity.BattleLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BattleLogRepository extends JpaRepository<BattleLog, Long> {
    List<BattleLog> findByAttackerIdOrDefenderIdOrderByCreatedAtDesc(Long attackerId, Long defenderId);
}
