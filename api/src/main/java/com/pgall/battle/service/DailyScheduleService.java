package com.pgall.battle.service;

import com.pgall.battle.entity.GameCharacter;
import com.pgall.battle.repository.GameCharacterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DailyScheduleService {

    private static final int DAILY_GOLD = 300;

    private final GameCharacterRepository characterRepository;
    private final ShopService shopService;
    private final HeroService heroService;

    /** 서버 시작 시 용사 초기화 */
    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        heroService.initHeroes();
    }

    /** 매일 0시: 모든 일일 작업 */
    @Scheduled(cron = "0 0 0 * * *")
    public void dailyReset() {
        log.info("=== 일일 초기화 시작 ===");

        // 1. 상점 세션 리셋
        shopService.resetSessions();
        log.info("상점 세션 초기화 완료.");

        // 2. 모든 캐릭터에게 300골드 지급
        grantDailyGold();

        // 3. 용사 일일 가챠
        heroService.dailyHeroGacha();

        log.info("=== 일일 초기화 완료 ===");
    }

    @Transactional
    public void grantDailyGold() {
        List<GameCharacter> all = characterRepository.findAll();
        for (GameCharacter c : all) {
            c.setGold(c.getGold() + DAILY_GOLD);
        }
        characterRepository.saveAll(all);
        log.info("전체 캐릭터 {}명에게 {}G 지급 완료.", all.size(), DAILY_GOLD);
    }
}
