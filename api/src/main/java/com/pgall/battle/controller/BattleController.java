package com.pgall.battle.controller;

import com.pgall.battle.dto.BattleRequest;
import com.pgall.battle.dto.BattleResponse;
import com.pgall.battle.entity.BattleLog;
import com.pgall.battle.entity.GameCharacter;
import com.pgall.battle.filter.IpOwnershipFilter;
import com.pgall.battle.repository.GameCharacterRepository;
import com.pgall.battle.service.BattleService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/battle")
@RequiredArgsConstructor
public class BattleController {

    private final BattleService battleService;
    private final GameCharacterRepository characterRepository;

    @PostMapping
    public ResponseEntity<BattleResponse> battle(@RequestBody BattleRequest request,
                                                  HttpServletRequest httpRequest) {
        // attacker가 요청자의 캐릭터인지 IP 검증
        String requestIp = IpOwnershipFilter.extractIp(httpRequest);
        GameCharacter attacker = characterRepository.findById(request.getAttackerId())
                .orElseThrow(() -> new NoSuchElementException("공격자 캐릭터를 찾을 수 없습니다."));
        if (attacker.getIpAddress() != null && !attacker.getIpAddress().equals(requestIp)) {
            throw new SecurityException("다른 유저의 캐릭터로 전투할 수 없습니다.");
        }

        return ResponseEntity.ok(battleService.battle(request));
    }

    @GetMapping("/logs/{characterId}")
    public ResponseEntity<List<BattleLog>> logs(@PathVariable Long characterId) {
        return ResponseEntity.ok(battleService.getLogs(characterId));
    }
}
