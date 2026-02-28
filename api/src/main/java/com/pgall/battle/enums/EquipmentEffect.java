package com.pgall.battle.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum EquipmentEffect {

    // ===== 무기 효과 (20개) =====
    FIRE_DAMAGE("화염 공격", Category.WEAPON),
    ICE_DAMAGE("빙결 공격", Category.WEAPON),
    LIGHTNING_DAMAGE("번개 공격", Category.WEAPON),
    HOLY_DAMAGE("신성 공격", Category.WEAPON),
    DARK_DAMAGE("암흑 공격", Category.WEAPON),
    ACID_DAMAGE("산성 공격", Category.WEAPON),
    ARMOR_PENETRATION("관통", Category.WEAPON),
    BLEEDING("출혈", Category.WEAPON),
    LIFE_STEAL("흡혈", Category.WEAPON),
    DOUBLE_ATTACK("더블 어택", Category.WEAPON),
    CRITICAL_BOOST("크리티컬 강화", Category.WEAPON),
    STUN_STRIKE("기절 타격", Category.WEAPON),
    KNOCKBACK("넉백", Category.WEAPON),
    VORPAL("참수", Category.WEAPON),
    DEBUFF_ATK_DOWN("공격력 감소", Category.WEAPON),
    DEBUFF_DEF_DOWN("방어력 감소", Category.WEAPON),
    SLOW("속도 감소", Category.WEAPON),
    SILENCE("침묵", Category.WEAPON),
    DISARM("무장 해제", Category.WEAPON),
    EXECUTE("처형", Category.WEAPON),

    // ===== 방어구 효과 (20개) =====
    BLOCK_CHANCE("공격 차단", Category.ARMOR),
    MAGIC_RESISTANCE("마법 저항", Category.ARMOR),
    THORNS("가시", Category.ARMOR),
    HP_REGEN("HP 재생", Category.ARMOR),
    DAMAGE_REDUCTION("피해 감소", Category.ARMOR),
    DODGE_BOOST("회피 증가", Category.ARMOR),
    FIRE_RESISTANCE("화염 저항", Category.ARMOR),
    ICE_RESISTANCE("빙결 저항", Category.ARMOR),
    LIGHTNING_RESISTANCE("번개 저항", Category.ARMOR),
    POISON_RESISTANCE("독 저항", Category.ARMOR),
    STUN_RESISTANCE("기절 저항", Category.ARMOR),
    REFLECT_MAGIC("마법 반사", Category.ARMOR),
    SECOND_WIND("재기", Category.ARMOR),
    HEAVY_ARMOR("중갑", Category.ARMOR),
    ENDURANCE("인내", Category.ARMOR),
    PERSEVERANCE("불굴", Category.ARMOR),
    IRON_SKIN("철피", Category.ARMOR),
    HEALING_AURA("치유 오라", Category.ARMOR),
    ABSORB_SHIELD("흡수 보호막", Category.ARMOR),
    FORTIFY("강화", Category.ARMOR),

    // ===== 악세사리 효과 (20개) =====
    ACCURACY_UP("명중률 증가", Category.ACCESSORY),
    COUNTER_ATTACK("반격", Category.ACCESSORY),
    POISON("독", Category.ACCESSORY),
    CURSE_WEAKNESS("허약 저주", Category.ACCESSORY),
    MANA_SHIELD("마나 보호막", Category.ACCESSORY),
    HASTE("가속", Category.ACCESSORY),
    LUCK("행운", Category.ACCESSORY),
    VAMPIRIC_AURA("흡혈 오라", Category.ACCESSORY),
    DEATH_WARD("죽음의 보호", Category.ACCESSORY),
    INTIMIDATE("위협", Category.ACCESSORY),
    BLESS("축복", Category.ACCESSORY),
    EVASION("완전 회피", Category.ACCESSORY),
    PIERCING_GAZE("꿰뚫는 시선", Category.ACCESSORY),
    SOUL_HARVEST("영혼 수확", Category.ACCESSORY),
    ARCANE_FOCUS("비전 집중", Category.ACCESSORY),
    DIVINE_FAVOR("신의 은총", Category.ACCESSORY),
    CHAOS_STRIKE("혼돈 일격", Category.ACCESSORY),
    ELEMENTAL_BOOST("원소 강화", Category.ACCESSORY),
    SPIRIT_LINK("영혼 연결", Category.ACCESSORY),
    MANA_DRAIN("마나 흡수", Category.ACCESSORY);

    private final String koreanName;
    private final Category category;

    public enum Category {
        WEAPON, ARMOR, ACCESSORY
    }
}
