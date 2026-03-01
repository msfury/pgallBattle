package com.pgall.battle.service;

import com.pgall.battle.dto.BattleRequest;
import com.pgall.battle.dto.BattleResponse;
import com.pgall.battle.entity.*;
import com.pgall.battle.enums.*;
import com.pgall.battle.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.ReentrantLock;

@Service
@RequiredArgsConstructor
public class BattleService {

    private static final ReentrantLock battleLock = new ReentrantLock();

    private final GameCharacterRepository characterRepository;
    private final InventoryRepository inventoryRepository;
    private final BattleLogRepository battleLogRepository;

    @Transactional
    public BattleResponse battle(BattleRequest request) {
        battleLock.lock();
        try {
            return doBattle(request);
        } finally {
            battleLock.unlock();
        }
    }

    private BattleResponse doBattle(BattleRequest request) {
        GameCharacter attacker = characterRepository.findById(request.getAttackerId())
                .orElseThrow(() -> new NoSuchElementException("공격자를 찾을 수 없습니다."));
        GameCharacter defender = characterRepository.findById(request.getDefenderId())
                .orElseThrow(() -> new NoSuchElementException("방어자를 찾을 수 없습니다."));

        if (attacker.getId().equals(defender.getId())) {
            throw new IllegalArgumentException("자기 자신과 전투할 수 없습니다.");
        }

        List<String> log = new ArrayList<>();
        ThreadLocalRandom random = ThreadLocalRandom.current();

        int atkHp = attacker.getMaxHp();
        int defHp = defender.getMaxHp();
        int atkMaxHp = attacker.getMaxHp();
        int defMaxHp = defender.getMaxHp();

        Equipment atkWeapon = getEquippedWeapon(attacker);
        Equipment defWeapon = getEquippedWeapon(defender);

        // === 전투 시작 ===
        log.add("=== 전투 시작 ===");
        String atkClass = attacker.getCharacterClass() != null ? " [" + attacker.getCharacterClass().getKoreanName() + "]" : "";
        String defClass = defender.getCharacterClass() != null ? " [" + defender.getCharacterClass().getKoreanName() + "]" : "";
        log.add(attacker.getName() + atkClass + " [HP:" + atkHp + "] vs " + defender.getName() + defClass + " [HP:" + defHp + "]");

        if (atkWeapon != null) {
            String info = atkWeapon.getWeaponCategory() != null ? atkWeapon.getWeaponCategory().getKoreanName() : "무기";
            log.add(attacker.getName() + "의 무기: " + atkWeapon.getName() + " (" + info + ")");
        }
        if (defWeapon != null) {
            String info = defWeapon.getWeaponCategory() != null ? defWeapon.getWeaponCategory().getKoreanName() : "무기";
            log.add(defender.getName() + "의 무기: " + defWeapon.getName() + " (" + info + ")");
        }

        // 물약 (장착된 물약만) 로드
        List<Inventory> atkPotions = inventoryRepository.findByCharacterIdAndEquipped(attacker.getId(), true);
        List<Inventory> defPotions = inventoryRepository.findByCharacterIdAndEquipped(defender.getId(), true);
        Set<BuffType> atkBuffsUsed = new HashSet<>();
        Set<BuffType> defBuffsUsed = new HashSet<>();

        // 보유 물약 표시 + 정보 수집
        showPotions(atkPotions, attacker.getName(), log);
        showPotions(defPotions, defender.getName(), log);
        List<BattleResponse.PotionInfo> atkPotionInfos = buildPotionInfos(atkPotions);
        List<BattleResponse.PotionInfo> defPotionInfos = buildPotionInfos(defPotions);

        // 장비 효과 수집 (effectChance → 발동확률, effectValue → 수치)
        Map<EquipmentEffect, int[]> atkEffects = collectEffects(attacker);
        Map<EquipmentEffect, int[]> defEffects = collectEffects(defender);

        // 장비 스탯 보너스
        int atkDexB = getEquipStatBonus(attacker, Equipment::getBonusDexterity);
        int defDexB = getEquipStatBonus(defender, Equipment::getBonusDexterity);
        int atkConB = getEquipStatBonus(attacker, Equipment::getBonusConstitution);
        int defConB = getEquipStatBonus(defender, Equipment::getBonusConstitution);

        // CON 보너스 → HP 증가
        if (atkConB > 0) { atkHp += atkConB * 2; atkMaxHp += atkConB * 2; }
        if (defConB > 0) { defHp += defConB * 2; defMaxHp += defConB * 2; }

        // AC 계산 (방어력 캡 8, DEX 장비보너스는 AC에 미반영)
        int atkAC = 10 + mod(attacker.getDexterity()) + Math.min(getTotalDefense(attacker), 8);
        int defAC = 10 + mod(defender.getDexterity()) + Math.min(getTotalDefense(defender), 8);

        // 전투 시작 전 효과 적용
        // DODGE_BOOST (절반만 반영)
        atkAC += getEffectValue(atkEffects, EquipmentEffect.DODGE_BOOST) / 2;
        defAC += getEffectValue(defEffects, EquipmentEffect.DODGE_BOOST) / 2;
        // FORTIFY (절반만 반영)
        atkAC += getEffectValue(atkEffects, EquipmentEffect.FORTIFY) / 2;
        defAC += getEffectValue(defEffects, EquipmentEffect.FORTIFY) / 2;
        // ENDURANCE (최대HP 증가)
        int atkEndurance = getEffectValue(atkEffects, EquipmentEffect.ENDURANCE);
        int defEndurance = getEffectValue(defEffects, EquipmentEffect.ENDURANCE);
        if (atkEndurance > 0) { atkHp += atkEndurance; atkMaxHp += atkEndurance; log.add(attacker.getName() + "의 인내! 최대HP +" + atkEndurance); }
        if (defEndurance > 0) { defHp += defEndurance; defMaxHp += defEndurance; log.add(defender.getName() + "의 인내! 최대HP +" + defEndurance); }
        // ABSORB_SHIELD
        int atkAbsorb = getEffectValue(atkEffects, EquipmentEffect.ABSORB_SHIELD);
        int defAbsorb = getEffectValue(defEffects, EquipmentEffect.ABSORB_SHIELD);
        if (atkAbsorb > 0) log.add(attacker.getName() + "의 흡수 보호막! 데미지 " + atkAbsorb + " 흡수");
        if (defAbsorb > 0) log.add(defender.getName() + "의 흡수 보호막! 데미지 " + defAbsorb + " 흡수");

        // 사전 디버프
        if (proc(atkEffects, EquipmentEffect.DEBUFF_DEF_DOWN, random)) {
            int v = val(atkEffects, EquipmentEffect.DEBUFF_DEF_DOWN);
            defAC -= v; log.add(attacker.getName() + "의 장비! " + defender.getName() + " 방어력 -" + v);
        }
        if (proc(defEffects, EquipmentEffect.DEBUFF_DEF_DOWN, random)) {
            int v = val(defEffects, EquipmentEffect.DEBUFF_DEF_DOWN);
            atkAC -= v; log.add(defender.getName() + "의 장비! " + attacker.getName() + " 방어력 -" + v);
        }
        int atkDebuff = proc(defEffects, EquipmentEffect.DEBUFF_ATK_DOWN, random) ? val(defEffects, EquipmentEffect.DEBUFF_ATK_DOWN) : 0;
        int defDebuff = proc(atkEffects, EquipmentEffect.DEBUFF_ATK_DOWN, random) ? val(atkEffects, EquipmentEffect.DEBUFF_ATK_DOWN) : 0;
        if (atkDebuff > 0) log.add(defender.getName() + "의 장비! " + attacker.getName() + " 공격력 -" + atkDebuff);
        if (defDebuff > 0) log.add(attacker.getName() + "의 장비! " + defender.getName() + " 공격력 -" + defDebuff);

        // CURSE_WEAKNESS
        boolean atkCursed = proc(defEffects, EquipmentEffect.CURSE_WEAKNESS, random);
        boolean defCursed = proc(atkEffects, EquipmentEffect.CURSE_WEAKNESS, random);
        if (atkCursed) log.add(defender.getName() + "의 허약 저주! " + attacker.getName() + " 데미지 30% 감소");
        if (defCursed) log.add(attacker.getName() + "의 허약 저주! " + defender.getName() + " 데미지 30% 감소");

        // INTIMIDATE
        if (proc(atkEffects, EquipmentEffect.INTIMIDATE, random)) {
            atkDebuff += 1; log.add(attacker.getName() + "의 위협! " + defender.getName() + " 위축!");
        }
        if (proc(defEffects, EquipmentEffect.INTIMIDATE, random)) {
            defDebuff += 1; log.add(defender.getName() + "의 위협! " + attacker.getName() + " 위축!");
        }

        // ACCURACY_UP
        int atkAccuracy = getEffectValue(atkEffects, EquipmentEffect.ACCURACY_UP);
        int defAccuracy = getEffectValue(defEffects, EquipmentEffect.ACCURACY_UP);
        // PIERCING_GAZE
        atkAccuracy += getEffectValue(atkEffects, EquipmentEffect.PIERCING_GAZE);
        defAccuracy += getEffectValue(defEffects, EquipmentEffect.PIERCING_GAZE);

        // BLESS
        if (hasEffect(atkEffects, EquipmentEffect.BLESS)) { atkAccuracy += 2; log.add(attacker.getName() + "의 축복! 모든 능력 강화!"); }
        if (hasEffect(defEffects, EquipmentEffect.BLESS)) { defAccuracy += 2; log.add(defender.getName() + "의 축복! 모든 능력 강화!"); }

        // 이니셔티브
        int atkInit = roll(20) + mod(attacker.getDexterity());
        int defInit = roll(20) + mod(defender.getDexterity());
        // HASTE
        if (hasEffect(atkEffects, EquipmentEffect.HASTE)) { atkInit += 5; log.add(attacker.getName() + "의 가속!"); }
        if (hasEffect(defEffects, EquipmentEffect.HASTE)) { defInit += 5; log.add(defender.getName() + "의 가속!"); }
        boolean attackerFirst = atkInit >= defInit;
        log.add("이니셔티브: " + attacker.getName() + "(" + atkInit + ") vs " + defender.getName() + "(" + defInit + ")");
        log.add((attackerFirst ? attacker.getName() : defender.getName()) + " 선공!");

        // 전투 상태
        int round = 1;
        boolean atkStunned = false, defStunned = false;
        boolean atkBleeding = false, defBleeding = false;
        boolean atkSlowed = false, defSlowed = false;
        boolean atkSilenced = false, defSilenced = false;
        boolean atkDisarmed = false, defDisarmed = false;
        boolean atkSecondWind = hasEffect(atkEffects, EquipmentEffect.SECOND_WIND);
        boolean defSecondWind = hasEffect(defEffects, EquipmentEffect.SECOND_WIND);
        boolean atkDeathWard = hasEffect(atkEffects, EquipmentEffect.DEATH_WARD);
        boolean defDeathWard = hasEffect(defEffects, EquipmentEffect.DEATH_WARD);

        while (atkHp > 0 && defHp > 0 && round <= 20) {
            log.add("--- 라운드 " + round + " ---");

            // 물약 자동 사용 (치유: HP 25% 이하, 버프: 1라운드)
            atkHp = usePotions(atkPotions, atkBuffsUsed, atkHp, atkMaxHp, attacker, log, random, round);
            defHp = usePotions(defPotions, defBuffsUsed, defHp, defMaxHp, defender, log, random, round);

            if (attackerFirst) {
                defHp = doTurn(attacker, defender, atkHp, defHp, atkMaxHp, defMaxHp, defAC,
                        log, random, atkEffects, defEffects, atkDebuff, atkAccuracy, atkWeapon, defWeapon,
                        atkStunned, atkBleeding, atkSlowed, atkSilenced, atkDisarmed, atkCursed,
                        atkBuffsUsed, round, atkAbsorb, defAbsorb, true);
                if (defHp <= 0) { defHp = checkRevive(defHp, defSecondWind, defDeathWard, defender.getName(), log); defSecondWind = false; defDeathWard = false; }
                if (defHp <= 0) break;

                // 상태이상 체크 (공격자→방어자)
                if (proc(atkEffects, EquipmentEffect.STUN_STRIKE, random)) {
                    if (!hasEffect(defEffects, EquipmentEffect.STUN_RESISTANCE) && !hasEffect(defEffects, EquipmentEffect.PERSEVERANCE)) {
                        defStunned = true; log.add(attacker.getName() + "의 효과! " + defender.getName() + " 기절!");
                    } else { log.add(defender.getName() + " 기절 저항!"); }
                }
                if (proc(atkEffects, EquipmentEffect.BLEEDING, random)) { defBleeding = true; log.add(defender.getName() + "에게 출혈!"); }
                if (proc(atkEffects, EquipmentEffect.SLOW, random)) { defSlowed = true; log.add(defender.getName() + " 속도 감소!"); }
                if (proc(atkEffects, EquipmentEffect.SILENCE, random)) { defSilenced = true; log.add(defender.getName() + " 침묵!"); }
                if (proc(atkEffects, EquipmentEffect.DISARM, random)) { defDisarmed = true; log.add(defender.getName() + " 무장 해제!"); }
                if (proc(atkEffects, EquipmentEffect.KNOCKBACK, random)) { log.add(defender.getName() + " 넉백! 다음 공격 명중률 감소"); defAccuracy -= 2; }

                // 방어자 턴
                atkHp = doTurn(defender, attacker, defHp, atkHp, defMaxHp, atkMaxHp, atkAC,
                        log, random, defEffects, atkEffects, defDebuff, defAccuracy, defWeapon, atkWeapon,
                        defStunned, defBleeding, defSlowed, defSilenced, defDisarmed, defCursed,
                        defBuffsUsed, round, defAbsorb, atkAbsorb, true);
                if (atkHp <= 0) { atkHp = checkRevive(atkHp, atkSecondWind, atkDeathWard, attacker.getName(), log); atkSecondWind = false; atkDeathWard = false; }
                if (atkHp <= 0) break;

                if (proc(defEffects, EquipmentEffect.STUN_STRIKE, random)) {
                    if (!hasEffect(atkEffects, EquipmentEffect.STUN_RESISTANCE) && !hasEffect(atkEffects, EquipmentEffect.PERSEVERANCE)) {
                        atkStunned = true; log.add(defender.getName() + "의 효과! " + attacker.getName() + " 기절!");
                    } else { log.add(attacker.getName() + " 기절 저항!"); }
                }
                if (proc(defEffects, EquipmentEffect.BLEEDING, random)) { atkBleeding = true; log.add(attacker.getName() + "에게 출혈!"); }
                if (proc(defEffects, EquipmentEffect.SLOW, random)) { atkSlowed = true; log.add(attacker.getName() + " 속도 감소!"); }
                if (proc(defEffects, EquipmentEffect.SILENCE, random)) { atkSilenced = true; log.add(attacker.getName() + " 침묵!"); }
                if (proc(defEffects, EquipmentEffect.DISARM, random)) { atkDisarmed = true; log.add(attacker.getName() + " 무장 해제!"); }
            } else {
                // 방어자 선공 (mirror)
                atkHp = doTurn(defender, attacker, defHp, atkHp, defMaxHp, atkMaxHp, atkAC,
                        log, random, defEffects, atkEffects, defDebuff, defAccuracy, defWeapon, atkWeapon,
                        defStunned, defBleeding, defSlowed, defSilenced, defDisarmed, defCursed,
                        defBuffsUsed, round, defAbsorb, atkAbsorb, true);
                if (atkHp <= 0) { atkHp = checkRevive(atkHp, atkSecondWind, atkDeathWard, attacker.getName(), log); atkSecondWind = false; atkDeathWard = false; }
                if (atkHp <= 0) break;
                if (proc(defEffects, EquipmentEffect.STUN_STRIKE, random)) {
                    if (!hasEffect(atkEffects, EquipmentEffect.STUN_RESISTANCE) && !hasEffect(atkEffects, EquipmentEffect.PERSEVERANCE)) {
                        atkStunned = true; log.add(defender.getName() + "의 효과! " + attacker.getName() + " 기절!");
                    }
                }
                if (proc(defEffects, EquipmentEffect.BLEEDING, random)) atkBleeding = true;
                if (proc(defEffects, EquipmentEffect.SLOW, random)) atkSlowed = true;

                defHp = doTurn(attacker, defender, atkHp, defHp, atkMaxHp, defMaxHp, defAC,
                        log, random, atkEffects, defEffects, atkDebuff, atkAccuracy, atkWeapon, defWeapon,
                        atkStunned, atkBleeding, atkSlowed, atkSilenced, atkDisarmed, atkCursed,
                        atkBuffsUsed, round, atkAbsorb, defAbsorb, true);
                if (defHp <= 0) { defHp = checkRevive(defHp, defSecondWind, defDeathWard, defender.getName(), log); defSecondWind = false; defDeathWard = false; }
                if (defHp <= 0) break;
                if (proc(atkEffects, EquipmentEffect.STUN_STRIKE, random)) {
                    if (!hasEffect(defEffects, EquipmentEffect.STUN_RESISTANCE) && !hasEffect(defEffects, EquipmentEffect.PERSEVERANCE)) {
                        defStunned = true; log.add(attacker.getName() + "의 효과! " + defender.getName() + " 기절!");
                    }
                }
                if (proc(atkEffects, EquipmentEffect.BLEEDING, random)) defBleeding = true;
                if (proc(atkEffects, EquipmentEffect.SLOW, random)) defSlowed = true;
            }

            // 라운드 종료 효과
            // 출혈 데미지
            if (atkBleeding) { int bleed = 2; atkHp -= bleed; log.add(attacker.getName() + " 출혈 데미지 " + bleed + "! (남은HP:" + Math.max(0, atkHp) + ")"); }
            if (defBleeding) { int bleed = 2; defHp -= bleed; log.add(defender.getName() + " 출혈 데미지 " + bleed + "! (남은HP:" + Math.max(0, defHp) + ")"); }
            // 독 데미지
            if (proc(atkEffects, EquipmentEffect.POISON, random)) { int v = val(atkEffects, EquipmentEffect.POISON); defHp -= v; log.add(defender.getName() + " 독 데미지 " + v + "! (남은HP:" + Math.max(0, defHp) + ")"); }
            if (proc(defEffects, EquipmentEffect.POISON, random)) { int v = val(defEffects, EquipmentEffect.POISON); atkHp -= v; log.add(attacker.getName() + " 독 데미지 " + v + "! (남은HP:" + Math.max(0, atkHp) + ")"); }
            // HP 재생
            if (hasEffect(atkEffects, EquipmentEffect.HP_REGEN) && atkHp > 0) { int v = val(atkEffects, EquipmentEffect.HP_REGEN); atkHp = Math.min(atkHp + v, atkMaxHp); log.add(attacker.getName() + " HP 재생 +" + v + " (HP:" + atkHp + ")"); }
            if (hasEffect(defEffects, EquipmentEffect.HP_REGEN) && defHp > 0) { int v = val(defEffects, EquipmentEffect.HP_REGEN); defHp = Math.min(defHp + v, defMaxHp); log.add(defender.getName() + " HP 재생 +" + v + " (HP:" + defHp + ")"); }
            // HEALING_AURA
            if (hasEffect(atkEffects, EquipmentEffect.HEALING_AURA) && atkHp > 0) { int v = 1; atkHp = Math.min(atkHp + v, atkMaxHp); log.add(attacker.getName() + " 치유 오라 +" + v + " (HP:" + atkHp + ")"); }
            if (hasEffect(defEffects, EquipmentEffect.HEALING_AURA) && defHp > 0) { int v = 1; defHp = Math.min(defHp + v, defMaxHp); log.add(defender.getName() + " 치유 오라 +" + v + " (HP:" + defHp + ")"); }
            // 성직자 힐
            atkHp = applyClassHeal(attacker, atkHp, atkMaxHp, log);
            defHp = applyClassHeal(defender, defHp, defMaxHp, log);
            // SPIRIT_LINK (공유 HP 밸런싱 - 자신 HP 1 회복)
            if (hasEffect(atkEffects, EquipmentEffect.SPIRIT_LINK) && atkHp > 0 && atkHp < atkMaxHp) { atkHp++; log.add(attacker.getName() + " 영혼 연결 +1 (HP:" + atkHp + ")"); }
            if (hasEffect(defEffects, EquipmentEffect.SPIRIT_LINK) && defHp > 0 && defHp < defMaxHp) { defHp++; log.add(defender.getName() + " 영혼 연결 +1 (HP:" + defHp + ")"); }

            // 상태이상 해제 (1턴 후)
            if (atkStunned) atkStunned = false;
            if (defStunned) defStunned = false;
            if (atkSlowed) atkSlowed = false;
            if (defSlowed) defSlowed = false;
            if (atkSilenced) atkSilenced = false;
            if (defSilenced) defSilenced = false;
            if (atkDisarmed) atkDisarmed = false;
            if (defDisarmed) defDisarmed = false;

            round++;
        }

        // 물약 소모 저장
        List<Inventory> allPotions = new ArrayList<>();
        allPotions.addAll(atkPotions);
        allPotions.addAll(defPotions);
        List<Inventory> depleted = allPotions.stream().filter(inv -> inv.getQuantity() <= 0).toList();
        List<Inventory> remaining = allPotions.stream().filter(inv -> inv.getQuantity() > 0).toList();
        if (!remaining.isEmpty()) inventoryRepository.saveAll(remaining);
        if (!depleted.isEmpty()) inventoryRepository.deleteAll(depleted);

        // 결과
        GameCharacter winner, loser;
        if (defHp <= 0) { winner = attacker; loser = defender; }
        else if (atkHp <= 0) { winner = defender; loser = attacker; }
        else {
            winner = atkHp >= defHp ? attacker : defender;
            loser = winner == attacker ? defender : attacker;
            log.add("20라운드 경과! HP가 더 많은 " + winner.getName() + " 판정승! ("
                    + attacker.getName() + " HP:" + Math.max(0, atkHp) + "/" + atkMaxHp
                    + " vs " + defender.getName() + " HP:" + Math.max(0, defHp) + "/" + defMaxHp + ")");
        }

        int goldReward = calcGoldReward(winner.getEloRate(), loser.getEloRate(), random);
        winner.setGold(winner.getGold() + goldReward);

        // SOUL_HARVEST
        if (winner == attacker && hasEffect(atkEffects, EquipmentEffect.SOUL_HARVEST)) {
            int heal = 5; log.add(attacker.getName() + "의 영혼 수확! HP +" + heal);
        } else if (winner == defender && hasEffect(defEffects, EquipmentEffect.SOUL_HARVEST)) {
            int heal = 5; log.add(defender.getName() + "의 영혼 수확! HP +" + heal);
        }

        // ELO 레이팅 (K=32)
        double expectedW = 1.0 / (1 + Math.pow(10, (loser.getEloRate() - winner.getEloRate()) / 400.0));
        int winnerDelta = (int) Math.round(32 * (1 - expectedW));
        int loserDelta = (int) Math.round(32 * (0 - (1 - expectedW)));
        winner.setEloRate(winner.getEloRate() + winnerDelta);
        loser.setEloRate(Math.max(0, loser.getEloRate() + loserDelta));
        characterRepository.save(winner);
        characterRepository.save(loser);

        log.add("=== 전투 종료 ===");
        log.add(winner.getName() + " 승리! +" + goldReward + " 골드");
        log.add("ELO: " + winner.getName() + " +" + winnerDelta + " (" + winner.getEloRate() + ") | "
                + loser.getName() + " " + loserDelta + " (" + loser.getEloRate() + ")");

        try {
            battleLogRepository.save(BattleLog.builder()
                    .attackerId(request.getAttackerId())
                    .defenderId(request.getDefenderId())
                    .winnerId(winner.getId())
                    .log(String.join("\n", log))
                    .build());
        } catch (Exception ignored) {}

        return BattleResponse.builder()
                .winnerId(winner.getId()).winnerName(winner.getName())
                .loserId(loser.getId()).loserName(loser.getName())
                .battleLog(log).goldReward(goldReward)
                .attackerName(attacker.getName()).defenderName(defender.getName())
                .attackerAvatar(attacker.getAvatar()).defenderAvatar(defender.getAvatar())
                .attackerClass(attacker.getCharacterClass() != null ? attacker.getCharacterClass().name() : null)
                .defenderClass(defender.getCharacterClass() != null ? defender.getCharacterClass().name() : null)
                .attackerMaxHp(atkMaxHp).defenderMaxHp(defMaxHp)
                .attackerFinalHp(Math.max(0, atkHp)).defenderFinalHp(Math.max(0, defHp))
                .attackerPotions(atkPotionInfos).defenderPotions(defPotionInfos)
                .build();
    }

    private List<BattleResponse.PotionInfo> buildPotionInfos(List<Inventory> potions) {
        return potions.stream()
                .filter(inv -> inv.getQuantity() > 0 && inv.getShopItem() != null)
                .map(inv -> BattleResponse.PotionInfo.builder()
                        .name(inv.getShopItem().getName())
                        .buffType(inv.getShopItem().getBuffType().name())
                        .quantity(inv.getQuantity())
                        .build())
                .toList();
    }

    /** 한 캐릭터의 턴 처리 */
    private int doTurn(GameCharacter atk, GameCharacter def, int atkHp, int defHp,
                       int atkMaxHp, int defMaxHp, int defAC,
                       List<String> log, ThreadLocalRandom random,
                       Map<EquipmentEffect, int[]> atkEffects, Map<EquipmentEffect, int[]> defEffects,
                       int atkDebuff, int accuracy, Equipment atkWeapon, Equipment defWeapon,
                       boolean stunned, boolean bleeding, boolean slowed, boolean silenced, boolean disarmed, boolean cursed,
                       Set<BuffType> buffsUsed, int round,
                       int atkAbsorb, int defAbsorb, boolean canAct) {
        if (stunned) {
            log.add(atk.getName() + "은(는) 기절 상태! 턴을 넘깁니다.");
            return defHp;
        }
        if (slowed && random.nextBoolean()) {
            log.add(atk.getName() + "은(는) 속도 감소로 행동 지연!");
            return defHp;
        }

        int attackCount = 1;
        // 더블 어택 (장비 또는 물약)
        if (proc(atkEffects, EquipmentEffect.DOUBLE_ATTACK, random) || buffsUsed.contains(BuffType.DOUBLE_ATTACK)) {
            attackCount = 2; log.add(atk.getName() + " 더블 어택!");
        }
        // HASTE 추가 공격
        if (hasEffect(atkEffects, EquipmentEffect.HASTE) && random.nextInt(100) < 30) {
            attackCount = 2; log.add(atk.getName() + " 가속 추가 공격!");
        }
        // 궁수 + 활
        if (atk.getCharacterClass() == CharacterClass.RANGER
                && atkWeapon != null && atkWeapon.getWeaponCategory() == WeaponCategory.BOW
                && random.nextInt(100) < 50) {
            attackCount = 2; log.add(atk.getName() + " 속사!");
        }

        int atkMod = disarmed ? 0 : getAttackModifier(atk, atkWeapon);
        int weaponHitBonus = disarmed ? 0 : getTotalAttack(atk) / 2;
        boolean shieldActive = buffsUsed.contains(BuffType.SHIELD);

        // 관통
        int penetration = getEffectValue(atkEffects, EquipmentEffect.ARMOR_PENETRATION);
        int effectiveAC = Math.max(5, defAC - penetration);

        for (int i = 0; i < attackCount; i++) {
            int attackRoll = roll(20);
            boolean isNat20 = attackRoll == 20;
            boolean crit = isNat20 || (buffsUsed.contains(BuffType.CRIT_DOUBLE) && attackRoll >= 19);
            // CRITICAL_BOOST
            if (!crit && hasEffect(atkEffects, EquipmentEffect.CRITICAL_BOOST) && attackRoll >= 18) crit = true;
            // LUCK
            if (!crit && hasEffect(atkEffects, EquipmentEffect.LUCK) && attackRoll >= 19) crit = true;

            int totalAttack = attackRoll + atkMod + weaponHitBonus - atkDebuff + accuracy;

            if (isNat20 || totalAttack >= effectiveAC) {
                // 방패
                if (shieldActive) { log.add(def.getName() + "의 방패 발동! 차단!"); shieldActive = false; continue; }
                // BLOCK_CHANCE
                if (proc(defEffects, EquipmentEffect.BLOCK_CHANCE, random)) { log.add(def.getName() + " 공격 차단!"); continue; }
                // EVASION
                if (proc(defEffects, EquipmentEffect.EVASION, random)) { log.add(def.getName() + " 완전 회피!"); continue; }
                // MANA_SHIELD
                if (proc(defEffects, EquipmentEffect.MANA_SHIELD, random)) {
                    int absorbed = val(defEffects, EquipmentEffect.MANA_SHIELD);
                    log.add(def.getName() + "의 마나 보호막! " + absorbed + " 데미지 흡수"); continue;
                }

                // 데미지 계산
                int damage = disarmed ? 1 : (rollWeaponDamage(atkWeapon, random) + atkMod + getWeaponAttack(atk) - atkDebuff);
                damage += getClassDamageBonus(atk, atkWeapon, round, random);
                if (damage < 1) damage = 1;
                if (crit) { damage = (int)(damage * 1.5); log.add("크리티컬 히트!"); }
                if (cursed) damage = (int)(damage * 0.7); // 허약 저주

                // 원소 추가 데미지
                damage += calcElementalDamage(atkEffects, defEffects, log, atk.getName(), def.getName(), random, silenced);

                // DAMAGE_REDUCTION
                int reduction = getEffectValue(defEffects, EquipmentEffect.DAMAGE_REDUCTION);
                // HEAVY_ARMOR
                reduction += getEffectValue(defEffects, EquipmentEffect.HEAVY_ARMOR);
                // IRON_SKIN (%)
                if (hasEffect(defEffects, EquipmentEffect.IRON_SKIN)) damage = (int)(damage * 0.85);
                damage = Math.max(1, damage - reduction);

                // 흡수 보호막
                if (defAbsorb > 0) {
                    int absorbed = Math.min(defAbsorb, damage);
                    damage -= absorbed;
                    defAbsorb -= absorbed;
                    if (absorbed > 0) log.add(def.getName() + "의 보호막이 " + absorbed + " 흡수!");
                }

                defHp -= damage;
                log.add(atk.getName() + " -> " + def.getName() + " (" + totalAttack + " vs AC" + effectiveAC + ") 명중! "
                        + damage + " 데미지 (남은HP:" + Math.max(0, defHp) + ")");

                // EXECUTE (HP 20% 이하 즉사)
                if (hasEffect(atkEffects, EquipmentEffect.EXECUTE) && defHp > 0 && defHp <= defMaxHp * 0.2) {
                    if (random.nextInt(100) < 30) { defHp = 0; log.add(atk.getName() + "의 처형! " + def.getName() + " 즉사!"); }
                }
                // VORPAL (크리 시 추가 대미지)
                if (crit && hasEffect(atkEffects, EquipmentEffect.VORPAL)) {
                    int vorpal = damage / 2; defHp -= vorpal; log.add(atk.getName() + "의 참수! 추가 " + vorpal + " 데미지!");
                }

                // 흡혈
                if (proc(atkEffects, EquipmentEffect.LIFE_STEAL, random)) {
                    int heal = damage / 3; if (heal > 0) log.add(atk.getName() + " 흡혈! +" + heal + " HP");
                }
                // VAMPIRIC_AURA
                if (hasEffect(atkEffects, EquipmentEffect.VAMPIRIC_AURA)) {
                    int heal = damage / 5; if (heal > 0) log.add(atk.getName() + " 흡혈 오라! +" + heal + " HP");
                }
                // MANA_DRAIN
                if (proc(atkEffects, EquipmentEffect.MANA_DRAIN, random)) {
                    log.add(atk.getName() + "의 마나 흡수! " + def.getName() + " 약화!");
                }

                // THORNS (피격 시 반사)
                if (hasEffect(defEffects, EquipmentEffect.THORNS)) {
                    int thorns = val(defEffects, EquipmentEffect.THORNS);
                    log.add(def.getName() + "의 가시! " + atk.getName() + "에게 반사 " + thorns + " 데미지");
                }
                // REFLECT_MAGIC
                if (proc(defEffects, EquipmentEffect.REFLECT_MAGIC, random)) {
                    int reflect = damage / 4;
                    log.add(def.getName() + "의 마법 반사! " + reflect + " 데미지 반사");
                }
                // COUNTER_ATTACK
                if (proc(defEffects, EquipmentEffect.COUNTER_ATTACK, random)) {
                    int counter = damage / 2;
                    log.add(def.getName() + "의 반격! " + counter + " 데미지!");
                }
                // CHAOS_STRIKE
                if (proc(atkEffects, EquipmentEffect.CHAOS_STRIKE, random)) {
                    int chaos = random.nextInt(1, 8);
                    defHp -= chaos; log.add(atk.getName() + "의 혼돈 일격! 추가 " + chaos + " 데미지!");
                }

                if (defHp <= 0) break;
            } else {
                log.add(atk.getName() + " -> " + def.getName() + " (" + totalAttack + " vs AC" + effectiveAC + ") 빗나감!");
            }
        }

        return defHp;
    }

    /** 원소 추가 데미지 계산 */
    private int calcElementalDamage(Map<EquipmentEffect, int[]> atkEffects, Map<EquipmentEffect, int[]> defEffects,
                                     List<String> log, String atkName, String defName,
                                     ThreadLocalRandom random, boolean silenced) {
        int extra = 0;
        // SILENCE 상태면 마법 원소 데미지 비활성
        if (proc(atkEffects, EquipmentEffect.FIRE_DAMAGE, random)) {
            int v = val(atkEffects, EquipmentEffect.FIRE_DAMAGE);
            if (hasEffect(defEffects, EquipmentEffect.FIRE_RESISTANCE)) { v /= 2; log.add(defName + " 화염 저항!"); }
            extra += v; log.add(atkName + " 화염 +" + v);
        }
        if (proc(atkEffects, EquipmentEffect.ICE_DAMAGE, random)) {
            int v = val(atkEffects, EquipmentEffect.ICE_DAMAGE);
            if (hasEffect(defEffects, EquipmentEffect.ICE_RESISTANCE)) { v /= 2; log.add(defName + " 빙결 저항!"); }
            extra += v; log.add(atkName + " 빙결 +" + v);
        }
        if (proc(atkEffects, EquipmentEffect.LIGHTNING_DAMAGE, random)) {
            int v = val(atkEffects, EquipmentEffect.LIGHTNING_DAMAGE);
            if (hasEffect(defEffects, EquipmentEffect.LIGHTNING_RESISTANCE)) { v /= 2; log.add(defName + " 번개 저항!"); }
            extra += v; log.add(atkName + " 번개 +" + v);
        }
        if (!silenced && proc(atkEffects, EquipmentEffect.HOLY_DAMAGE, random)) {
            int v = val(atkEffects, EquipmentEffect.HOLY_DAMAGE);
            if (hasEffect(defEffects, EquipmentEffect.MAGIC_RESISTANCE)) v /= 2;
            extra += v; log.add(atkName + " 신성 +" + v);
        }
        if (!silenced && proc(atkEffects, EquipmentEffect.DARK_DAMAGE, random)) {
            int v = val(atkEffects, EquipmentEffect.DARK_DAMAGE);
            if (hasEffect(defEffects, EquipmentEffect.MAGIC_RESISTANCE)) v /= 2;
            extra += v; log.add(atkName + " 암흑 +" + v);
        }
        if (proc(atkEffects, EquipmentEffect.ACID_DAMAGE, random)) {
            int v = val(atkEffects, EquipmentEffect.ACID_DAMAGE);
            extra += v; log.add(atkName + " 산성 +" + v);
        }
        // ELEMENTAL_BOOST
        if (extra > 0 && hasEffect(atkEffects, EquipmentEffect.ELEMENTAL_BOOST)) {
            int bonus = extra / 3;
            extra += bonus; log.add(atkName + " 원소 강화 +" + bonus);
        }
        // ARCANE_FOCUS
        if (extra > 0 && hasEffect(atkEffects, EquipmentEffect.ARCANE_FOCUS)) {
            int bonus = extra / 4;
            extra += bonus;
        }
        // DIVINE_FAVOR
        if (hasEffect(atkEffects, EquipmentEffect.DIVINE_FAVOR) && proc(atkEffects, EquipmentEffect.DIVINE_FAVOR, random)) {
            extra += 2; log.add(atkName + " 신의 은총!");
        }
        return extra;
    }

    /** 물약 자동 사용 */
    private int usePotions(List<Inventory> potions, Set<BuffType> usedBuffs, int hp, int maxHp,
                           GameCharacter character, List<String> log, ThreadLocalRandom random, int round) {
        for (Inventory inv : potions) {
            if (inv.getQuantity() <= 0) continue;
            ShopItem item = inv.getShopItem();
            if (item == null) continue;
            BuffType buff = item.getBuffType();

            if (buff.isHealType()) {
                // 치유 물약: HP 25% 이하일 때 자동 사용
                if (hp <= maxHp * 0.25 && hp > 0) {
                    int heal = 5 + mod(character.getConstitution());
                    if (buff == BuffType.GREATER_HEAL) heal = 10 + mod(character.getConstitution()) * 2;
                    hp = Math.min(hp + heal, maxHp);
                    inv.setQuantity(inv.getQuantity() - 1);
                    log.add(character.getName() + "의 " + item.getName() + " 사용! HP +" + heal + " (HP:" + hp + ")");
                }
            } else {
                // 버프 물약: 1라운드에 1회 사용
                if (round == 1 && !usedBuffs.contains(buff)) {
                    usedBuffs.add(buff);
                    inv.setQuantity(inv.getQuantity() - 1);
                    log.add(character.getName() + "의 " + item.getName() + " 사용! (" + buff.getKoreanName() + ")");
                }
            }
        }
        return hp;
    }

    private void showPotions(List<Inventory> potions, String name, List<String> log) {
        List<String> items = new ArrayList<>();
        for (Inventory inv : potions) {
            if (inv.getQuantity() > 0 && inv.getShopItem() != null) {
                items.add(inv.getShopItem().getName() + " x" + inv.getQuantity());
            }
        }
        if (!items.isEmpty()) {
            log.add(name + "의 보유 아이템: " + String.join(", ", items));
        }
    }

    private int checkRevive(int hp, boolean secondWind, boolean deathWard, String name, List<String> log) {
        if (hp <= 0 && secondWind) { hp = 1; log.add(name + "의 재기! HP 1로 부활!"); }
        if (hp <= 0 && deathWard) { hp = 1; log.add(name + "의 죽음의 보호! 즉사 방지!"); }
        return hp;
    }

    // ===== 유틸리티 =====

    /** effect 보유 여부 (확률 무시) */
    private boolean hasEffect(Map<EquipmentEffect, int[]> effects, EquipmentEffect effect) {
        return effects.containsKey(effect);
    }

    /** effectValue 가져오기 (없으면 0) */
    private int getEffectValue(Map<EquipmentEffect, int[]> effects, EquipmentEffect effect) {
        int[] v = effects.get(effect);
        return v != null ? v[1] : 0;
    }

    /** 효과 확률 발동 체크 */
    private boolean proc(Map<EquipmentEffect, int[]> effects, EquipmentEffect effect, ThreadLocalRandom random) {
        int[] v = effects.get(effect);
        return v != null && random.nextInt(100) < v[0];
    }

    /** effectValue (shorthand) */
    private int val(Map<EquipmentEffect, int[]> effects, EquipmentEffect effect) {
        int[] v = effects.get(effect);
        return v != null ? v[1] : 0;
    }

    /** 장착된 장비 효과 수집 → {effect: [chance, value]} (기본 + 강화 효과) */
    private Map<EquipmentEffect, int[]> collectEffects(GameCharacter character) {
        Map<EquipmentEffect, int[]> effects = new HashMap<>();
        for (Equipment eq : character.getEquipments()) {
            if (!eq.isEquipped()) continue;
            // 레거시 단일 효과
            if (eq.getEffect() != null) {
                effects.merge(eq.getEffect(),
                        new int[]{eq.getEffectChance(), eq.getEffectValue()},
                        (a, b) -> new int[]{Math.max(a[0], b[0]), a[1] + b[1]});
            }
            // 기본 효과 (등급별)
            for (var be : eq.getBaseEffects()) {
                effects.merge(be.getEffect(),
                        new int[]{be.getEffectChance(), be.getEffectValue()},
                        (a, b) -> new int[]{Math.max(a[0], b[0]), a[1] + b[1]});
            }
            // 강화 효과
            for (var ee : eq.getEnhanceEffects()) {
                effects.merge(ee.getEffect(),
                        new int[]{ee.getEffectChance(), ee.getEffectValue()},
                        (a, b) -> new int[]{Math.max(a[0], b[0]), a[1] + b[1]});
            }
        }
        return effects;
    }

    private Equipment getEquippedWeapon(GameCharacter character) {
        return character.getEquipments().stream()
                .filter(e -> e.isEquipped() && e.getType() == EquipmentType.WEAPON)
                .findFirst().orElse(null);
    }

    private int getAttackModifier(GameCharacter character, Equipment weapon) {
        if (weapon == null || weapon.getScalingStat() == null) return mod(character.getStrength());
        return switch (weapon.getScalingStat()) {
            case STR -> mod(character.getStrength()) + mod(character.getDexterity()); // 힘 + 민첩 보정
            case DEX -> mod(character.getDexterity()) + 2;   // 민첩 무기 명중 보정
            case INT -> mod(character.getIntelligence()) + 2; // 마법 무기 명중 보정
            case WIS -> mod(character.getWisdom()) + 2;       // 신성 무기 명중 보정
        };
    }

    private int rollWeaponDamage(Equipment weapon, ThreadLocalRandom random) {
        if (weapon == null || weapon.getBaseDamageMax() == 0) return 1;
        return random.nextInt(weapon.getBaseDamageMin(), weapon.getBaseDamageMax() + 1);
    }

    private int getClassDamageBonus(GameCharacter atk, Equipment weapon, int round, ThreadLocalRandom random) {
        if (atk.getCharacterClass() == null) return 0;
        return switch (atk.getCharacterClass()) {
            case WARRIOR -> (weapon != null && weapon.getWeaponCategory() != null
                    && !weapon.getWeaponCategory().isMagical()
                    && weapon.getWeaponCategory() != WeaponCategory.BOW) ? 2 : 0;
            case ROGUE -> round == 1 ? random.nextInt(1, 7) : 0;
            case MAGE -> (weapon != null && weapon.getWeaponCategory() != null
                    && weapon.getWeaponCategory().isMagical()) ? 3 : 0;
            case RANGER -> (weapon != null && weapon.getWeaponCategory() == WeaponCategory.BOW) ? 2 : 0;
            case CLERIC -> 0;
        };
    }

    private int applyClassHeal(GameCharacter character, int hp, int maxHp, List<String> log) {
        if (hp <= 0) return hp;
        if (character.getCharacterClass() == CharacterClass.CLERIC) {
            int heal = 1 + mod(character.getWisdom());
            if (heal > 0 && hp < maxHp) {
                hp = Math.min(hp + heal, maxHp);
                log.add(character.getName() + "의 신성한 힘! HP +" + heal + " 회복 (HP:" + hp + ")");
            }
        }
        return hp;
    }

    private int getTotalAttack(GameCharacter c) {
        return c.getEquipments().stream().filter(Equipment::isEquipped).mapToInt(Equipment::getAttackBonus).sum();
    }

    private int getWeaponAttack(GameCharacter c) {
        return c.getEquipments().stream()
                .filter(e -> e.isEquipped() && e.getType() == EquipmentType.WEAPON)
                .mapToInt(Equipment::getAttackBonus).sum();
    }

    private int getTotalDefense(GameCharacter c) {
        return c.getEquipments().stream().filter(Equipment::isEquipped).mapToInt(Equipment::getDefenseBonus).sum();
    }

    /** ELO 기반 골드 보상: 높은 ELO일수록 보상 증가, 상대 ELO가 높을수록 보너스 */
    private int calcGoldReward(int winnerElo, int loserElo, ThreadLocalRandom random) {
        // 기본 보상: ELO 구간별
        int base;
        if (winnerElo >= 1500) base = 30;
        else if (winnerElo >= 1200) base = 20;
        else base = 10;

        // 상대가 나보다 강할수록 보너스 (최대 +20)
        int eloDiff = loserElo - winnerElo;
        int bonus = Math.max(0, Math.min(20, eloDiff / 20));

        return base + bonus + random.nextInt(10);
    }

    /** 장착된 장비의 특정 스탯 보너스 합계 */
    private int getEquipStatBonus(GameCharacter c, java.util.function.ToIntFunction<Equipment> getter) {
        return c.getEquipments().stream().filter(Equipment::isEquipped).mapToInt(getter).sum();
    }

    private int mod(int stat) { return (stat - 10) / 2; }
    private int roll(int sides) { return ThreadLocalRandom.current().nextInt(1, sides + 1); }

    public List<BattleLog> getLogs(Long characterId) {
        return battleLogRepository.findByAttackerIdOrDefenderIdOrderByCreatedAtDesc(characterId, characterId);
    }
}
