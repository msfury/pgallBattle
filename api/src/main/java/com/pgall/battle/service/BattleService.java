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

@Service
@RequiredArgsConstructor
public class BattleService {

    private final GameCharacterRepository characterRepository;
    private final InventoryRepository inventoryRepository;
    private final BattleLogRepository battleLogRepository;

    @Transactional
    public BattleResponse battle(BattleRequest request) {
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

        Equipment atkWeapon = getEquippedWeapon(attacker);
        Equipment defWeapon = getEquippedWeapon(defender);

        log.add("=== 전투 시작 ===");
        String atkClass = attacker.getCharacterClass() != null ? " [" + attacker.getCharacterClass().getKoreanName() + "]" : "";
        String defClass = defender.getCharacterClass() != null ? " [" + defender.getCharacterClass().getKoreanName() + "]" : "";
        log.add(attacker.getName() + atkClass + " (HP:" + atkHp + ") vs " + defender.getName() + defClass + " (HP:" + defHp + ")");

        if (atkWeapon != null) {
            String atkWepInfo = atkWeapon.getWeaponCategory() != null
                    ? atkWeapon.getWeaponCategory().getKoreanName() : "무기";
            log.add(attacker.getName() + "의 무기: " + atkWeapon.getName()
                    + " (" + atkWepInfo + ", " + atkWeapon.getScalingStat() + ")");
        }
        if (defWeapon != null) {
            String defWepInfo = defWeapon.getWeaponCategory() != null
                    ? defWeapon.getWeaponCategory().getKoreanName() : "무기";
            log.add(defender.getName() + "의 무기: " + defWeapon.getName()
                    + " (" + defWepInfo + ", " + defWeapon.getScalingStat() + ")");
        }

        // 버프 확인
        Set<BuffType> atkBuffs = resolveBuffs(attacker.getId(), log, attacker.getName());
        Set<BuffType> defBuffs = resolveBuffs(defender.getId(), log, defender.getName());

        // 장비 특수효과 (장착된 것만)
        Map<EquipmentEffect, Integer> atkEffects = collectEffects(attacker);
        Map<EquipmentEffect, Integer> defEffects = collectEffects(defender);

        // AC 계산
        int atkAC = 10 + mod(attacker.getDexterity()) + getTotalDefense(attacker);
        int defAC = 10 + mod(defender.getDexterity()) + getTotalDefense(defender);

        // 직업 보너스 AC
        if (attacker.getCharacterClass() == CharacterClass.CLERIC) atkAC += 1;
        if (defender.getCharacterClass() == CharacterClass.CLERIC) defAC += 1;
        if (attacker.getCharacterClass() == CharacterClass.ROGUE) atkAC += 1;
        if (defender.getCharacterClass() == CharacterClass.ROGUE) defAC += 1;

        // 디버프 적용
        if (hasEffectProc(atkEffects, EquipmentEffect.DEBUFF_DEF_DOWN, random)) {
            int reduction = atkEffects.get(EquipmentEffect.DEBUFF_DEF_DOWN);
            defAC -= reduction;
            log.add(attacker.getName() + "의 장비 효과! " + defender.getName() + "의 방어력 -" + reduction);
        }
        if (hasEffectProc(defEffects, EquipmentEffect.DEBUFF_DEF_DOWN, random)) {
            int reduction = defEffects.get(EquipmentEffect.DEBUFF_DEF_DOWN);
            atkAC -= reduction;
            log.add(defender.getName() + "의 장비 효과! " + attacker.getName() + "의 방어력 -" + reduction);
        }

        int atkDebuff = hasEffectProc(defEffects, EquipmentEffect.DEBUFF_ATK_DOWN, random)
                ? defEffects.getOrDefault(EquipmentEffect.DEBUFF_ATK_DOWN, 0) : 0;
        int defDebuff = hasEffectProc(atkEffects, EquipmentEffect.DEBUFF_ATK_DOWN, random)
                ? atkEffects.getOrDefault(EquipmentEffect.DEBUFF_ATK_DOWN, 0) : 0;

        if (atkDebuff > 0) log.add(defender.getName() + "의 장비 효과! " + attacker.getName() + "의 공격력 감소!");
        if (defDebuff > 0) log.add(attacker.getName() + "의 장비 효과! " + defender.getName() + "의 공격력 감소!");

        // ACCURACY_UP 보너스
        int atkAccuracy = hasEffectProc(atkEffects, EquipmentEffect.ACCURACY_UP, random)
                ? atkEffects.getOrDefault(EquipmentEffect.ACCURACY_UP, 0) : 0;
        int defAccuracy = hasEffectProc(defEffects, EquipmentEffect.ACCURACY_UP, random)
                ? defEffects.getOrDefault(EquipmentEffect.ACCURACY_UP, 0) : 0;

        if (atkAccuracy > 0) log.add(attacker.getName() + "의 장갑 효과! 명중률 +" + atkAccuracy);
        if (defAccuracy > 0) log.add(defender.getName() + "의 장갑 효과! 명중률 +" + defAccuracy);

        // 치유 물약 버프
        if (atkBuffs.contains(BuffType.HEAL)) {
            int heal = 5 + mod(attacker.getConstitution());
            atkHp = Math.min(atkHp + heal, attacker.getMaxHp());
            log.add(attacker.getName() + " 치유 물약! HP +" + heal + " (HP:" + atkHp + ")");
        }
        if (defBuffs.contains(BuffType.HEAL)) {
            int heal = 5 + mod(defender.getConstitution());
            defHp = Math.min(defHp + heal, defender.getMaxHp());
            log.add(defender.getName() + " 치유 물약! HP +" + heal + " (HP:" + defHp + ")");
        }

        // 이니셔티브
        int atkInit = roll(20) + mod(attacker.getDexterity());
        int defInit = roll(20) + mod(defender.getDexterity());
        boolean attackerFirst = atkInit >= defInit;
        log.add("이니셔티브: " + attacker.getName() + "(" + atkInit + ") vs " + defender.getName() + "(" + defInit + ")");
        log.add((attackerFirst ? attacker.getName() : defender.getName()) + " 선공!");

        int round = 1;
        boolean atkStunned = false;
        boolean defStunned = false;

        while (atkHp > 0 && defHp > 0 && round <= 20) {
            log.add("--- 라운드 " + round + " ---");

            if (attackerFirst) {
                // 공격자 턴
                if (!atkStunned) {
                    defHp = performAttack(attacker, defender, defAC, defHp, log, random,
                            atkBuffs, atkEffects, defEffects, atkDebuff, atkAccuracy, atkWeapon, round);
                    if (defHp > 0) {
                        defStunned = checkStun(atkEffects, log, attacker.getName(), defender.getName(), random);
                    }
                    defHp = applyPoison(atkEffects, defHp, log, defender.getName(), random);
                } else {
                    log.add(attacker.getName() + "은(는) 기절 상태! 턴을 넘깁니다.");
                    atkStunned = false;
                }

                if (defHp <= 0) break;

                // 방어자 턴
                if (!defStunned) {
                    atkHp = performAttack(defender, attacker, atkAC, atkHp, log, random,
                            defBuffs, defEffects, atkEffects, defDebuff, defAccuracy, defWeapon, round);
                    if (atkHp > 0) {
                        atkStunned = checkStun(defEffects, log, defender.getName(), attacker.getName(), random);
                    }
                    atkHp = applyPoison(defEffects, atkHp, log, attacker.getName(), random);
                } else {
                    log.add(defender.getName() + "은(는) 기절 상태! 턴을 넘깁니다.");
                    defStunned = false;
                }
            } else {
                // 방어자 선공
                if (!defStunned) {
                    atkHp = performAttack(defender, attacker, atkAC, atkHp, log, random,
                            defBuffs, defEffects, atkEffects, defDebuff, defAccuracy, defWeapon, round);
                    if (atkHp > 0) {
                        atkStunned = checkStun(defEffects, log, defender.getName(), attacker.getName(), random);
                    }
                    atkHp = applyPoison(defEffects, atkHp, log, attacker.getName(), random);
                } else {
                    log.add(defender.getName() + "은(는) 기절 상태! 턴을 넘깁니다.");
                    defStunned = false;
                }

                if (atkHp <= 0) break;

                if (!atkStunned) {
                    defHp = performAttack(attacker, defender, defAC, defHp, log, random,
                            atkBuffs, atkEffects, defEffects, atkDebuff, atkAccuracy, atkWeapon, round);
                    if (defHp > 0) {
                        defStunned = checkStun(atkEffects, log, attacker.getName(), defender.getName(), random);
                    }
                    defHp = applyPoison(atkEffects, defHp, log, defender.getName(), random);
                } else {
                    log.add(attacker.getName() + "은(는) 기절 상태! 턴을 넘깁니다.");
                    atkStunned = false;
                }
            }

            // 성직자 라운드 종료 시 회복
            atkHp = applyClassHeal(attacker, atkHp, attacker.getMaxHp(), log);
            defHp = applyClassHeal(defender, defHp, defender.getMaxHp(), log);

            round++;
        }

        // 결과
        GameCharacter winner;
        GameCharacter loser;
        if (defHp <= 0) {
            winner = attacker;
            loser = defender;
        } else if (atkHp <= 0) {
            winner = defender;
            loser = attacker;
        } else {
            winner = atkHp >= defHp ? attacker : defender;
            loser = winner == attacker ? defender : attacker;
            log.add("20라운드 경과! HP가 더 많은 " + winner.getName() + " 판정승!");
        }

        int goldReward = 10 + random.nextInt(20);
        winner.setGold(winner.getGold() + goldReward);

        // ELO 레이팅 계산 (K=32)
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
            BattleLog battleLog = BattleLog.builder()
                    .attackerId(request.getAttackerId())
                    .defenderId(request.getDefenderId())
                    .winnerId(winner.getId())
                    .log(String.join("\n", log))
                    .build();
            battleLogRepository.save(battleLog);
        } catch (Exception ignored) {}

        return BattleResponse.builder()
                .winnerId(winner.getId())
                .winnerName(winner.getName())
                .loserId(loser.getId())
                .loserName(loser.getName())
                .battleLog(log)
                .goldReward(goldReward)
                .attackerName(attacker.getName())
                .defenderName(defender.getName())
                .attackerAvatar(attacker.getAvatar())
                .defenderAvatar(defender.getAvatar())
                .attackerClass(attacker.getCharacterClass() != null ? attacker.getCharacterClass().name() : null)
                .defenderClass(defender.getCharacterClass() != null ? defender.getCharacterClass().name() : null)
                .attackerMaxHp(attacker.getMaxHp())
                .defenderMaxHp(defender.getMaxHp())
                .build();
    }

    public List<BattleLog> getLogs(Long characterId) {
        return battleLogRepository.findByAttackerIdOrDefenderIdOrderByCreatedAtDesc(characterId, characterId);
    }

    private int performAttack(GameCharacter atk, GameCharacter def, int defAC, int defHp,
                              List<String> log, ThreadLocalRandom random,
                              Set<BuffType> buffs, Map<EquipmentEffect, Integer> atkEffects,
                              Map<EquipmentEffect, Integer> defEffects, int atkDebuff,
                              int accuracyBonus, Equipment weapon, int round) {
        int attackCount = 1;
        if (buffs.contains(BuffType.DOUBLE_ATTACK) || hasEffectProc(atkEffects, EquipmentEffect.DOUBLE_ATTACK, random)) {
            attackCount = 2;
            log.add(atk.getName() + " 더블 어택 발동!");
        }
        // 궁수 + 활 = 추가 공격 기회 (50%)
        if (atk.getCharacterClass() == CharacterClass.RANGER
                && weapon != null && weapon.getWeaponCategory() == WeaponCategory.BOW
                && random.nextInt(100) < 50) {
            attackCount = 2;
            log.add(atk.getName() + " 속사 발동!");
        }

        int atkMod = getAttackModifier(atk, weapon);

        // 방패 버프 (한번 차단)
        boolean shieldActive = buffs.contains(BuffType.SHIELD);

        for (int i = 0; i < attackCount; i++) {
            int attackRoll = roll(20);
            boolean crit = attackRoll == 20 || (buffs.contains(BuffType.CRIT_DOUBLE) && attackRoll >= 19);
            int totalAttack = attackRoll + atkMod - atkDebuff + accuracyBonus;

            if (totalAttack >= defAC) {
                // 방패 버프 차단
                if (shieldActive) {
                    log.add(def.getName() + "의 수호의 방패 발동! 공격 차단!");
                    shieldActive = false;
                    continue;
                }
                // 블록 체크
                if (hasEffectProc(defEffects, EquipmentEffect.BLOCK_CHANCE, random)) {
                    log.add(def.getName() + "의 장비가 공격을 차단!");
                    continue;
                }

                int damage = rollWeaponDamage(weapon, random) + atkMod + getTotalAttack(atk) - atkDebuff;
                damage += getClassDamageBonus(atk, weapon, round, random);
                if (damage < 1) damage = 1;
                if (crit) {
                    damage *= 2;
                    log.add("크리티컬 히트!");
                }

                defHp -= damage;
                log.add(atk.getName() + " -> " + def.getName() + " (" + attackRoll + " vs AC" + defAC + ") 명중! "
                        + damage + " 데미지 (남은HP:" + Math.max(0, defHp) + ")");

                // 흡혈
                if (hasEffectProc(atkEffects, EquipmentEffect.LIFE_STEAL, random)) {
                    int heal = damage / 3;
                    if (heal > 0) {
                        log.add(atk.getName() + " 생명력 흡수! +" + heal + " HP");
                    }
                }
            } else {
                log.add(atk.getName() + " -> " + def.getName() + " (" + attackRoll + " vs AC" + defAC + ") 빗나감!");
            }
        }

        return defHp;
    }

    private Equipment getEquippedWeapon(GameCharacter character) {
        return character.getEquipments().stream()
                .filter(e -> e.isEquipped() && e.getType() == EquipmentType.WEAPON)
                .findFirst()
                .orElse(null);
    }

    private int getAttackModifier(GameCharacter character, Equipment weapon) {
        if (weapon == null || weapon.getScalingStat() == null) {
            return mod(character.getStrength());
        }
        return switch (weapon.getScalingStat()) {
            case STR -> mod(character.getStrength());
            case DEX -> mod(character.getDexterity());
            case INT -> mod(character.getIntelligence());
            case WIS -> mod(character.getWisdom());
        };
    }

    private int rollWeaponDamage(Equipment weapon, ThreadLocalRandom random) {
        if (weapon == null || weapon.getBaseDamageMax() == 0) return 1;
        return random.nextInt(weapon.getBaseDamageMin(), weapon.getBaseDamageMax() + 1);
    }

    private int getClassDamageBonus(GameCharacter character, Equipment weapon, int round, ThreadLocalRandom random) {
        if (character.getCharacterClass() == null) return 0;
        return switch (character.getCharacterClass()) {
            case WARRIOR -> {
                // 근접무기(비마법, 비활) 데미지 +2
                if (weapon != null && weapon.getWeaponCategory() != null
                        && !weapon.getWeaponCategory().isMagical()
                        && weapon.getWeaponCategory() != WeaponCategory.BOW) {
                    yield 2;
                }
                yield 0;
            }
            case ROGUE -> {
                // 첫 라운드 급소공격 +1d6
                if (round == 1) {
                    yield random.nextInt(1, 7);
                }
                yield 0;
            }
            case MAGE -> {
                // 마법무기(지팡이/완드) 데미지 +3
                if (weapon != null && weapon.getWeaponCategory() != null && weapon.getWeaponCategory().isMagical()) {
                    yield 3;
                }
                yield 0;
            }
            case RANGER -> {
                // 활 데미지 +2
                if (weapon != null && weapon.getWeaponCategory() == WeaponCategory.BOW) {
                    yield 2;
                }
                yield 0;
            }
            case CLERIC -> 0; // 회복은 별도 처리
        };
    }

    private int applyClassHeal(GameCharacter character, int currentHp, int maxHp, List<String> log) {
        if (currentHp <= 0) return currentHp;
        if (character.getCharacterClass() == CharacterClass.CLERIC) {
            int healAmount = 1 + mod(character.getWisdom());
            if (healAmount > 0 && currentHp < maxHp) {
                currentHp = Math.min(currentHp + healAmount, maxHp);
                log.add(character.getName() + "의 신성한 힘! HP +" + healAmount + " 회복 (HP:" + currentHp + ")");
            }
        }
        return currentHp;
    }

    private Set<BuffType> resolveBuffs(Long characterId, List<String> log, String name) {
        Set<BuffType> activeBuffs = new HashSet<>();
        List<Inventory> items = inventoryRepository.findByCharacterId(characterId);
        ThreadLocalRandom random = ThreadLocalRandom.current();

        for (Inventory inv : items) {
            if (inv.getQuantity() > 0) {
                ShopItem item = inv.getShopItem();
                if (random.nextInt(100) < item.getBuffChance()) {
                    activeBuffs.add(item.getBuffType());
                    log.add(name + "의 " + item.getName() + " 발동! (" + item.getBuffType() + ")");
                    inv.setQuantity(inv.getQuantity() - 1);
                }
            }
        }
        return activeBuffs;
    }

    private Map<EquipmentEffect, Integer> collectEffects(GameCharacter character) {
        Map<EquipmentEffect, Integer> effects = new HashMap<>();
        for (Equipment eq : character.getEquipments()) {
            if (!eq.isEquipped()) continue;
            if (eq.getEffect() != null) {
                effects.put(eq.getEffect(), eq.getEffectChance());
            }
        }
        return effects;
    }

    private boolean hasEffectProc(Map<EquipmentEffect, Integer> effects, EquipmentEffect effect, ThreadLocalRandom random) {
        Integer chance = effects.get(effect);
        return chance != null && random.nextInt(100) < chance;
    }

    private boolean checkStun(Map<EquipmentEffect, Integer> effects, List<String> log,
                              String attackerName, String targetName, ThreadLocalRandom random) {
        if (hasEffectProc(effects, EquipmentEffect.STUN, random)) {
            log.add(attackerName + "의 장비 효과! " + targetName + " 기절!");
            return true;
        }
        return false;
    }

    private int applyPoison(Map<EquipmentEffect, Integer> effects, int hp, List<String> log,
                            String targetName, ThreadLocalRandom random) {
        if (hasEffectProc(effects, EquipmentEffect.POISON, random)) {
            int poisonDmg = effects.get(EquipmentEffect.POISON);
            hp -= poisonDmg;
            log.add(targetName + "에게 독 데미지 " + poisonDmg + "! (남은HP:" + Math.max(0, hp) + ")");
        }
        return hp;
    }

    private int getTotalAttack(GameCharacter character) {
        return character.getEquipments().stream()
                .filter(Equipment::isEquipped)
                .mapToInt(Equipment::getAttackBonus)
                .sum();
    }

    private int getTotalDefense(GameCharacter character) {
        return character.getEquipments().stream()
                .filter(Equipment::isEquipped)
                .mapToInt(Equipment::getDefenseBonus)
                .sum();
    }

    private int mod(int stat) {
        return (stat - 10) / 2;
    }

    private int roll(int sides) {
        return ThreadLocalRandom.current().nextInt(1, sides + 1);
    }
}
