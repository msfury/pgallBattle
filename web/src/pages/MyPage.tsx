import { useEffect, useState, useCallback } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { api, type Character, type Equipment, type InventoryItem, type EnhanceResult, type EffectOption } from '../api/client';
import { CLASS_EMOJI, CLASS_COLOR, CLASS_TOOLTIP } from '../data/classes';
import SpriteAvatar from '../components/SpriteAvatar';

const SELL_PRICE: Record<string, number> = {
  COMMON: 5, UNCOMMON: 10, RARE: 20, EPIC: 100, LEGENDARY: 200,
};

const GRADE_CLASS: Record<string, string> = {
  COMMON: 'grade-common', UNCOMMON: 'grade-uncommon', RARE: 'grade-rare',
  EPIC: 'grade-epic', LEGENDARY: 'grade-legendary',
};

const GRADE_ORDER: Record<string, number> = {
  LEGENDARY: 0, EPIC: 1, RARE: 2, UNCOMMON: 3, COMMON: 4,
};

function equipScore(eq: Equipment): number {
  const gradeVal: Record<string, number> = { COMMON: 0, UNCOMMON: 1, RARE: 2, EPIC: 3, LEGENDARY: 4 };
  return (eq.attackBonus + eq.defenseBonus) * (1 + eq.enhanceLevel * 0.3) + (gradeVal[eq.grade] ?? 0);
}

const GRADE_LABEL: Record<string, string> = {
  LEGENDARY: '전설', EPIC: '에픽', RARE: '레어', UNCOMMON: '언커먼', COMMON: '커먼',
};

const EFFECT_NAMES: Record<string, string> = {
  FIRE_DAMAGE: '화염 공격', ICE_DAMAGE: '빙결 공격', LIGHTNING_DAMAGE: '번개 공격',
  HOLY_DAMAGE: '신성 공격', DARK_DAMAGE: '암흑 공격', ACID_DAMAGE: '산성 공격',
  ARMOR_PENETRATION: '관통', BLEEDING: '출혈', LIFE_STEAL: '흡혈', DOUBLE_ATTACK: '더블 어택',
  CRITICAL_BOOST: '크리티컬 강화', STUN_STRIKE: '기절 타격', KNOCKBACK: '넉백', VORPAL: '참수',
  DEBUFF_ATK_DOWN: '공격력 감소', DEBUFF_DEF_DOWN: '방어력 감소', SLOW: '속도 감소',
  SILENCE: '침묵', DISARM: '무장 해제', EXECUTE: '처형',
  BLOCK_CHANCE: '공격 차단', MAGIC_RESISTANCE: '마법 저항', THORNS: '가시', HP_REGEN: 'HP 재생',
  DAMAGE_REDUCTION: '피해 감소', DODGE_BOOST: '회피 증가', FIRE_RESISTANCE: '화염 저항',
  ICE_RESISTANCE: '빙결 저항', LIGHTNING_RESISTANCE: '번개 저항', POISON_RESISTANCE: '독 저항',
  STUN_RESISTANCE: '기절 저항', REFLECT_MAGIC: '마법 반사', SECOND_WIND: '재기',
  HEAVY_ARMOR: '중갑', ENDURANCE: '인내', PERSEVERANCE: '불굴', IRON_SKIN: '철피',
  HEALING_AURA: '치유 오라', ABSORB_SHIELD: '흡수 보호막', FORTIFY: '강화',
  ACCURACY_UP: '명중률 증가', COUNTER_ATTACK: '반격', POISON: '독', CURSE_WEAKNESS: '허약 저주',
  MANA_SHIELD: '마나 보호막', HASTE: '가속', LUCK: '행운', VAMPIRIC_AURA: '흡혈 오라',
  DEATH_WARD: '죽음의 보호', INTIMIDATE: '위협', BLESS: '축복', EVASION: '완전 회피',
  PIERCING_GAZE: '꿰뚫는 시선', SOUL_HARVEST: '영혼 수확', ARCANE_FOCUS: '비전 집중',
  DIVINE_FAVOR: '신의 은총', CHAOS_STRIKE: '혼돈 일격', ELEMENTAL_BOOST: '원소 강화',
  SPIRIT_LINK: '영혼 연결', MANA_DRAIN: '마나 흡수',
};

const EFFECT_TOOLTIPS: Record<string, string> = {
  FIRE_DAMAGE: '확률적으로 화염 추가 데미지',
  ICE_DAMAGE: '확률적으로 빙결 추가 데미지',
  LIGHTNING_DAMAGE: '확률적으로 번개 추가 데미지',
  HOLY_DAMAGE: '확률적으로 신성 추가 데미지',
  DARK_DAMAGE: '확률적으로 암흑 추가 데미지',
  ACID_DAMAGE: '확률적으로 산성 추가 데미지',
  ARMOR_PENETRATION: '상대 AC를 무시하여 관통',
  BLEEDING: '확률적으로 출혈 부여 (매 라운드 2 데미지)',
  LIFE_STEAL: '공격 시 확률적으로 HP 흡수',
  DOUBLE_ATTACK: '확률적으로 2회 연속 공격',
  CRITICAL_BOOST: '크리티컬 범위 확장 (18+)',
  STUN_STRIKE: '확률적으로 기절 (1턴 행동불가)',
  KNOCKBACK: '확률적으로 넉백 (명중률 감소)',
  VORPAL: '크리티컬 시 추가 50% 데미지',
  DEBUFF_ATK_DOWN: '전투 시작 시 상대 공격력 감소',
  DEBUFF_DEF_DOWN: '전투 시작 시 상대 방어력 감소',
  SLOW: '확률적으로 속도 감소 (행동 지연)',
  SILENCE: '확률적으로 침묵 (마법 데미지 차단)',
  DISARM: '확률적으로 무장 해제 (데미지 1 고정)',
  EXECUTE: 'HP 20% 이하 시 30% 확률 즉사',
  BLOCK_CHANCE: '확률적으로 공격 차단',
  MAGIC_RESISTANCE: '마법 데미지 50% 감소',
  THORNS: '피격 시 반사 데미지',
  HP_REGEN: '매 라운드 HP 재생',
  DAMAGE_REDUCTION: '받는 데미지 고정 감소',
  DODGE_BOOST: '회피율(AC) 증가',
  FIRE_RESISTANCE: '화염 데미지 50% 감소',
  ICE_RESISTANCE: '빙결 데미지 50% 감소',
  LIGHTNING_RESISTANCE: '번개 데미지 50% 감소',
  POISON_RESISTANCE: '독 데미지 면역',
  STUN_RESISTANCE: '기절 면역',
  REFLECT_MAGIC: '확률적으로 마법 데미지 25% 반사',
  SECOND_WIND: 'HP 0 시 1회 HP 1로 부활',
  HEAVY_ARMOR: '받는 데미지 고정 감소',
  ENDURANCE: '전투 시작 시 최대HP 증가',
  PERSEVERANCE: '기절 면역',
  IRON_SKIN: '받는 데미지 15% 감소',
  HEALING_AURA: '매 라운드 HP 1 회복',
  ABSORB_SHIELD: '전투 시작 시 데미지 흡수 보호막',
  FORTIFY: 'AC 증가',
  ACCURACY_UP: '명중률 증가',
  COUNTER_ATTACK: '확률적으로 50% 데미지 반격',
  POISON: '확률적으로 독 데미지',
  CURSE_WEAKNESS: '전투 시작 시 상대 데미지 30% 감소',
  MANA_SHIELD: '확률적으로 데미지 흡수',
  HASTE: '이니셔티브 +5, 30% 추가 공격',
  LUCK: '크리티컬 범위 확장 (19+)',
  VAMPIRIC_AURA: '공격 적중 시 데미지의 20% HP 흡수',
  DEATH_WARD: 'HP 0 시 1회 즉사 방지',
  INTIMIDATE: '전투 시작 시 상대 위축 (공격력 감소)',
  BLESS: '명중률 +2, 전체 능력 강화',
  EVASION: '확률적으로 완전 회피',
  PIERCING_GAZE: '명중률 증가',
  SOUL_HARVEST: '전투 승리 시 HP +5',
  ARCANE_FOCUS: '원소 데미지 25% 증가',
  DIVINE_FAVOR: '확률적으로 신성 추가 데미지 +2',
  CHAOS_STRIKE: '확률적으로 1~7 랜덤 추가 데미지',
  ELEMENTAL_BOOST: '원소 데미지 33% 증가',
  SPIRIT_LINK: '매 라운드 HP 1 회복',
  MANA_DRAIN: '확률적으로 상대 약화',
};

const TYPE_EMOJI: Record<string, string> = {
  WEAPON: '⚔️', HELMET: '🪖', ARMOR: '🛡️', GLOVES: '🧤', SHOES: '👢', EARRING: '💎', RING: '💍',
};

const TYPE_LABEL: Record<string, string> = {
  WEAPON: '무기', HELMET: '투구', ARMOR: '갑옷', GLOVES: '장갑', SHOES: '신발', EARRING: '귀걸이', RING: '반지',
};

const SLOT_LAYOUT = [
  { type: 'WEAPON', label: '무기 1' },
  { type: 'WEAPON', label: '무기 2' },
  { type: 'HELMET', label: '투구' },
  { type: 'ARMOR', label: '갑옷' },
  { type: 'GLOVES', label: '장갑' },
  { type: 'SHOES', label: '신발' },
  { type: 'EARRING', label: '귀걸이 1' },
  { type: 'EARRING', label: '귀걸이 2' },
  { type: 'RING', label: '반지 1' },
  { type: 'RING', label: '반지 2' },
];

const POTION_EMOJI: Record<string, string> = {
  HEAL: '❤️', GREATER_HEAL: '💖', CRIT_DOUBLE: '🎯', DOUBLE_ATTACK: '⚡',
  SHIELD: '🛡️', FIRE_ENCHANT: '🔥', ICE_ENCHANT: '❄️', LIGHTNING_ENCHANT: '⚡',
  HOLY_ENCHANT: '✨', PENETRATION_BOOST: '🗡️', REGEN_POTION: '💚', REFLECT_POTION: '🪞',
  ACCURACY_POTION: '🎯', HASTE_POTION: '💨', IRON_SKIN_POTION: '🛡️', BLESS_POTION: '🙏',
};

const TYPE_GROUPS = [
  { label: '무기', types: ['WEAPON'] },
  { label: '방어구', types: ['HELMET', 'ARMOR', 'GLOVES', 'SHOES'] },
  { label: '악세서리', types: ['EARRING', 'RING'] },
];

export default function MyPage() {
  const { id } = useParams<{ id: string }>();
  const myId = Number(id);
  const navigate = useNavigate();

  const [char, setChar] = useState<Character | null>(null);
  const [error, setError] = useState('');
  const [toast, setToast] = useState('');
  const [isOwner, setIsOwner] = useState(false);
  const [myCharId, setMyCharId] = useState<number | null>(null);

  // 장비 비교 모달 상태
  const [compareModal, setCompareModal] = useState<{ newEquip: Equipment; oldEquip: Equipment } | null>(null);

  // 강화 모달 상태
  const [enhanceTarget, setEnhanceTarget] = useState<Equipment | null>(null);
  const [enhanceInfo, setEnhanceInfo] = useState<EnhanceResult | null>(null);
  const [enhanceModalOpen, setEnhanceModalOpen] = useState(false);
  // 효과 선택 상태
  const [effectSelectionMode, setEffectSelectionMode] = useState(false);
  const [candidateEffects, setCandidateEffects] = useState<EffectOption[]>([]);
  const [currentEnhanceEffects, setCurrentEnhanceEffects] = useState<EffectOption[]>([]);
  const [maxEnhanceEffects, setMaxEnhanceEffects] = useState(0);
  const [selectedEffects, setSelectedEffects] = useState<Set<string>>(new Set());
  const [enhanceBroken, setEnhanceBroken] = useState(false);

  const loadChar = useCallback(() => api.getCharacter(myId).then(setChar).catch(e => {
    setError(e instanceof Error ? e.message : '로딩 실패');
  }), [myId]);

  useEffect(() => { loadChar(); }, [loadChar]);

  useEffect(() => {
    api.getMyCharacter()
      .then(c => {
        setMyCharId(c.id);
        setIsOwner(c.id === myId);
      })
      .catch(() => {
        setIsOwner(false);
        setMyCharId(null);
      });
  }, [myId]);

  useEffect(() => {
    if (!isOwner) return;
    api.dailyCheck(myId).then(res => {
      if (res.granted) {
        setToast(`오늘 일급이 발급되었어요! +${res.amount}G`);
        loadChar();
        const timer = setTimeout(() => setToast(''), 4000);
        return () => clearTimeout(timer);
      }
    }).catch(() => {});
  }, [myId, isOwner]);

  const doEquip = async (equipId: number) => {
    try {
      setError('');
      await api.equipItem(myId, equipId);
      await loadChar();
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : '장착 실패');
    }
  };

  const handleEquip = (equipId: number) => {
    if (!char) return;
    const newEquip = char.equipments.find(e => e.id === equipId);
    if (!newEquip) return;

    // 같은 타입의 장착 중인 장비 찾기
    const equipped = char.equipments.filter(e => e.type === newEquip.type && e.equipped);
    if (equipped.length === 0) {
      doEquip(equipId);
      return;
    }

    // 교체 대상 찾기 (백엔드 로직과 동일)
    let target: Equipment | null = null;
    if (newEquip.type === 'WEAPON') {
      const hasTwoHanded = equipped.find(e => e.twoHanded);
      if (newEquip.twoHanded) {
        // 양손무기 장착 → 기존 중 가장 좋은 것과 비교
        target = equipped.reduce((a, b) => equipScore(a) > equipScore(b) ? a : b);
      } else if (hasTwoHanded) {
        target = hasTwoHanded;
      } else if (equipped.length >= 2) {
        // 한손 2개 → 점수 낮은 것이 교체 대상
        target = equipped.reduce((a, b) => equipScore(a) < equipScore(b) ? a : b);
      } else {
        // 슬롯 여유 있음
        doEquip(equipId);
        return;
      }
    } else {
      const maxSlots = newEquip.type === 'EARRING' || newEquip.type === 'RING' ? 2 : 1;
      if (equipped.length < maxSlots) {
        doEquip(equipId);
        return;
      }
      target = equipped.reduce((a, b) => equipScore(a) < equipScore(b) ? a : b);
    }

    if (target && equipScore(newEquip) < equipScore(target)) {
      setCompareModal({ newEquip, oldEquip: target });
    } else {
      doEquip(equipId);
    }
  };

  const handleUnequip = async (equipId: number) => {
    try {
      setError('');
      await api.unequipItem(myId, equipId);
      await loadChar();
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : '해제 실패');
    }
  };

  const handleSell = async (eq: Equipment) => {
    const price = SELL_PRICE[eq.grade] || 5;
    const isRareOrAbove = (GRADE_ORDER[eq.grade] ?? 4) <= 2;
    if (isRareOrAbove && !confirm(`"${eq.name}"을(를) ${price}G에 판매하시겠습니까?`)) return;
    try {
      setError('');
      await api.sellEquipment(myId, eq.id);
      await loadChar();
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : '판매 실패');
    }
  };

  const handleEquipPotion = async (invId: number) => {
    try {
      setError('');
      await api.equipPotion(myId, invId);
      await loadChar();
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : '물약 장착 실패');
    }
  };

  const handleUnequipPotion = async (invId: number) => {
    try {
      setError('');
      await api.unequipPotion(myId, invId);
      await loadChar();
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : '물약 해제 실패');
    }
  };

  const handleSellPotion = async (potion: InventoryItem) => {
    try {
      setError('');
      await api.sellPotion(myId, potion.id);
      await loadChar();
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : '물약 판매 실패');
    }
  };

  // 강화 모달 열기
  const openEnhance = async (eq: Equipment) => {
    try {
      const info = await api.enhanceInfo(myId, eq.id);
      setEnhanceTarget(eq);
      setEnhanceInfo(info);
      setEnhanceModalOpen(true);
      setEffectSelectionMode(false);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : '강화 정보 조회 실패');
    }
  };

  const closeEnhanceModal = () => {
    setEnhanceModalOpen(false);
    setEnhanceTarget(null);
    setEnhanceInfo(null);
    setEffectSelectionMode(false);
    setSelectedEffects(new Set());
    setEnhanceBroken(false);
  };

  const handleEnhance = async () => {
    if (!enhanceTarget) return;
    try {
      setError('');
      const result = await api.enhance(myId, enhanceTarget.id);
      if (result.broken) {
        setEnhanceBroken(true);
        await loadChar();
        setTimeout(() => {
          closeEnhanceModal();
          setToast(result.message);
          setTimeout(() => setToast(''), 3000);
        }, 2000);
        return;
      } else if (result.success) {
        setToast(result.message);
        // 효과 선택이 필요한 경우
        if (result.needsEffectSelection && result.candidateEffects) {
          setEffectSelectionMode(true);
          setCandidateEffects(result.candidateEffects);
          setCurrentEnhanceEffects(result.currentEnhanceEffects || []);
          setMaxEnhanceEffects(result.maxEnhanceEffects);
          // 기존 효과 선택 상태로 초기화
          const initial = new Set<string>();
          (result.currentEnhanceEffects || []).forEach(e => initial.add(e.effect));
          setSelectedEffects(initial);
        } else {
          // 효과 선택 불필요 → 정보 갱신
          const refreshed = await api.getCharacter(myId);
          const updatedEq = refreshed.equipments.find(e => e.id === enhanceTarget.id);
          if (updatedEq) setEnhanceTarget(updatedEq);
          const info = await api.enhanceInfo(myId, enhanceTarget.id);
          setEnhanceInfo(info);
        }
      } else {
        setToast(result.message);
      }
      await loadChar();
      setTimeout(() => setToast(''), 3000);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : '강화 실패');
    }
  };

  const toggleEffectSelection = (effectName: string) => {
    setSelectedEffects(prev => {
      const next = new Set(prev);
      if (next.has(effectName)) {
        next.delete(effectName);
      } else if (next.size < maxEnhanceEffects) {
        next.add(effectName);
      }
      return next;
    });
  };

  const confirmEffectSelection = async () => {
    if (!enhanceTarget) return;
    try {
      await api.confirmEnhanceEffects(myId, enhanceTarget.id, Array.from(selectedEffects));
      setToast('강화 효과가 적용되었습니다!');
      setEffectSelectionMode(false);
      await loadChar();
      // 모달 정보 갱신
      const refreshed = await api.getCharacter(myId);
      const updatedEq = refreshed.equipments.find(e => e.id === enhanceTarget.id);
      if (updatedEq) setEnhanceTarget(updatedEq);
      const info = await api.enhanceInfo(myId, enhanceTarget.id);
      setEnhanceInfo(info);
      setTimeout(() => setToast(''), 3000);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : '효과 선택 실패');
    }
  };

  if (!char) return <div className="text-center mt-16">{error || '로딩 중...'}</div>;

  const equippedItems = char.equipments.filter(e => e.equipped);
  const unequippedItems = char.equipments.filter(e => !e.equipped);

  const equippedWeapons = equippedItems.filter(e => e.type === 'WEAPON');
  const twoHandedWeapon = equippedWeapons.find(e => e.twoHanded);

  const slotItems: (Equipment | null)[] = [];
  const usedIds = new Set<number>();
  for (const slot of SLOT_LAYOUT) {
    if (slot.type === 'WEAPON' && slot.label === '무기 2' && twoHandedWeapon) {
      slotItems.push(twoHandedWeapon);
    } else {
      const item = equippedItems.find(e => e.type === slot.type && !usedIds.has(e.id));
      if (item) usedIds.add(item.id);
      slotItems.push(item || null);
    }
  }

  const equippedPotions = (char.potions || []).filter(p => p.equipped);
  const unequippedPotions = (char.potions || []).filter(p => !p.equipped);
  const potionSlots: (InventoryItem | null)[] = [];
  for (let i = 0; i < 5; i++) {
    potionSlots.push(equippedPotions[i] || null);
  }

  const sortedUnequipped = [...unequippedItems].sort((a, b) => {
    const ga = GRADE_ORDER[a.grade] ?? 4;
    const gb = GRADE_ORDER[b.grade] ?? 4;
    return ga - gb;
  });

  const stats = [
    { label: 'STR 힘', value: char.strength }, { label: 'DEX 민첩', value: char.dexterity },
    { label: 'CON 체력', value: char.constitution }, { label: 'INT 지능', value: char.intelligence },
    { label: 'WIS 지혜', value: char.wisdom }, { label: 'CHA 매력', value: char.charisma },
  ];

  const renderEffects = (eq: Equipment) => {
    const allEffects = [
      ...(eq.baseEffects || []).map(e => ({ ...e, source: 'base' as const })),
      ...(eq.enhanceEffects || []).map(e => ({ ...e, source: 'enhance' as const })),
    ];
    if (eq.effect && !allEffects.length) {
      return (
        <div className="tooltip-wrap" style={{ fontSize: '0.6rem', color: '#f39c12' }}>
          [{EFFECT_NAMES[eq.effect] || eq.effect} {eq.effectChance}%]
          {EFFECT_TOOLTIPS[eq.effect] && <span className="tooltip-text">{EFFECT_TOOLTIPS[eq.effect]}</span>}
        </div>
      );
    }
    return allEffects.map((e, i) => (
      <div key={i} className="tooltip-wrap" style={{ fontSize: '0.6rem', color: e.source === 'enhance' ? '#e74c3c' : '#f39c12' }}>
        [{EFFECT_NAMES[e.effect] || e.effect} {e.effectChance}%]{e.source === 'enhance' ? ' +강화' : ''}
        {EFFECT_TOOLTIPS[e.effect] && <span className="tooltip-text">{EFFECT_TOOLTIPS[e.effect]}</span>}
      </div>
    ));
  };

  const renderEquipStats = (eq: Equipment) => {
    const statBonuses: string[] = [];
    if (eq.bonusStrength > 0) statBonuses.push(`STR+${eq.bonusStrength}`);
    if (eq.bonusDexterity > 0) statBonuses.push(`DEX+${eq.bonusDexterity}`);
    if (eq.bonusConstitution > 0) statBonuses.push(`CON+${eq.bonusConstitution}`);
    if (eq.bonusIntelligence > 0) statBonuses.push(`INT+${eq.bonusIntelligence}`);
    if (eq.bonusWisdom > 0) statBonuses.push(`WIS+${eq.bonusWisdom}`);
    if (eq.bonusCharisma > 0) statBonuses.push(`CHA+${eq.bonusCharisma}`);
    return (
      <div style={{ fontSize: '0.65rem', color: '#999', marginTop: 2 }}>
        {eq.attackBonus > 0 && <span style={{ color: '#e74c3c' }}>ATK+{eq.attackBonus} </span>}
        {eq.defenseBonus > 0 && <span style={{ color: '#3498db' }}>DEF+{eq.defenseBonus} </span>}
        {eq.type === 'WEAPON' && eq.baseDamageMax > 0 && (
          <span style={{ color: '#f39c12' }}>DMG:{eq.baseDamageMin}-{eq.baseDamageMax} </span>
        )}
        {eq.enhanceLevel > 0 && (
          <span style={{ color: '#ffd700' }}>+{eq.enhanceLevel} </span>
        )}
        {statBonuses.length > 0 && (
          <span style={{ color: '#2ecc71' }}>{statBonuses.join(' ')} </span>
        )}
      </div>
    );
  };

  return (
    <div>
      {/* 토스트 */}
      {toast && (
        <div style={{
          position: 'fixed', top: 16, left: '50%', transform: 'translateX(-50%)',
          background: 'linear-gradient(135deg, #f39c12, #e67e22)', color: '#fff',
          padding: '12px 24px', borderRadius: 12, fontWeight: 'bold', fontSize: '0.95rem',
          zIndex: 20000, boxShadow: '0 4px 20px rgba(243,156,18,0.4)',
          animation: 'toast-slide 0.3s ease-out',
        }}>
          {toast}
        </div>
      )}

      <button className="back-btn" onClick={() => navigate('/')}>← 홈</button>

      {/* 캐릭터 정보 */}
      <div className="card mb-12">
        <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
          <SpriteAvatar avatarId={char.avatar} animation="idle" scale={0.7} />
          <div style={{ flex: 1 }}>
            <h1 style={{ textAlign: 'left', marginBottom: 4 }}>{char.name}</h1>
            <div style={{ fontSize: '0.85rem', color: '#999' }}>
              Lv.{char.level}
              {char.characterClass && (
                <span className="tooltip-wrap" style={{ color: CLASS_COLOR[char.characterClass] || '#999', marginLeft: 6 }}>
                  {CLASS_EMOJI[char.characterClass]} {char.classKoreanName}
                  {CLASS_TOOLTIP[char.characterClass] && (
                    <span className="tooltip-text">{CLASS_TOOLTIP[char.characterClass]}</span>
                  )}
                </span>
              )}
            </div>
          </div>
          <div style={{ textAlign: 'right' }}>
            <div>HP: {char.hp}/{char.maxHp}</div>
            <div className="gold">💰 {char.gold} G</div>
            <div style={{ color: '#ffd700', fontWeight: 'bold' }}>ELO {char.eloRate}</div>
          </div>
        </div>
      </div>

      {/* 네비게이션 */}
      {isOwner && (
        <div className="flex-row mb-12">
          <button className="btn-gold" onClick={() => navigate(`/shop/${myId}`)}>🏪 상점</button>
          <button className="btn-blue" onClick={() => navigate(`/gacha/${myId}`)}>🎰 가챠</button>
        </div>
      )}
      {!isOwner && myCharId && (
        <div className="flex-row mb-12">
          <button className="btn-red" onClick={() => navigate(`/battle/${myCharId}/${myId}`)}>
            ⚔️ 전투하기
          </button>
        </div>
      )}

      {/* 능력치 */}
      <div className="card mb-12">
        <h2>능력치</h2>
        <div className="stat-grid">
          {stats.map(s => (
            <div className="stat-item" key={s.label}>
              <span className="stat-label">{s.label}</span>
              <span className="stat-value">{s.value}</span>
            </div>
          ))}
        </div>
      </div>

      {/* 장착 장비 */}
      <div className="card mb-12">
        <h2>장착 장비</h2>
        <div className="equip-grid">
          {SLOT_LAYOUT.map((slot, i) => {
            const item = slotItems[i];
            const isTwoHandedSlot2 = slot.label === '무기 2' && twoHandedWeapon;
            return (
              <div key={`${slot.type}-${i}`} className={`equip-slot ${item ? 'filled' : ''}`}>
                <div className="slot-label">{TYPE_EMOJI[slot.type]} {slot.label}</div>
                {item ? (
                  <div>
                    <div className={GRADE_CLASS[item.grade]} style={{ fontSize: '0.8rem', fontWeight: 'bold' }}>
                      {item.name}
                    </div>
                    {renderEquipStats(item)}
                    {renderEffects(item)}
                    {isTwoHandedSlot2 && (
                      <div style={{ fontSize: '0.6rem', color: '#9b59b6' }}>(양손무기)</div>
                    )}
                    {isOwner && !isTwoHandedSlot2 && (
                      <div style={{ display: 'flex', gap: 3, justifyContent: 'center', marginTop: 4 }}>
                        <button className="btn-sm btn-red"
                          onClick={() => handleUnequip(item.id)}>해제</button>
                        <button className="btn-sm btn-blue"
                          onClick={() => openEnhance(item)}>강화</button>
                      </div>
                    )}
                  </div>
                ) : (
                  <div className="slot-empty">비어있음</div>
                )}
              </div>
            );
          })}
        </div>
      </div>

      {/* 물약 장착 슬롯 */}
      <div className="card mb-12">
        <h2>🧪 물약 슬롯</h2>
        <div className="potion-slot-grid">
          {potionSlots.map((potion, i) => (
            <div key={`potion-slot-${i}`} className={`potion-slot ${potion ? 'filled' : ''}`}>
              {potion ? (
                <div style={{ textAlign: 'center' }}>
                  <div style={{ fontSize: '1.2rem' }}>{POTION_EMOJI[potion.buffType || ''] || '🧪'}</div>
                  <div style={{ fontSize: '0.65rem', fontWeight: 'bold', color: '#2ecc71', marginTop: 2 }}>
                    {potion.name}
                  </div>
                  <div style={{ fontSize: '0.6rem', color: '#999' }}>x{potion.quantity}</div>
                  {isOwner && (
                    <div style={{ display: 'flex', gap: 3, justifyContent: 'center', marginTop: 4 }}>
                      <button className="btn-sm btn-red" onClick={() => handleUnequipPotion(potion.id)}
                        style={{ fontSize: '0.6rem', padding: '2px 6px' }}>해제</button>
                      <button className="btn-sm" onClick={() => handleSellPotion(potion)}
                        style={{ fontSize: '0.6rem', padding: '2px 6px', background: '#e67e22', color: '#fff', border: 'none', cursor: 'pointer', borderRadius: 4 }}>
                        {potion.sellPrice}G
                      </button>
                    </div>
                  )}
                </div>
              ) : (
                <div className="slot-empty" style={{ fontSize: '0.7rem' }}>빈 슬롯</div>
              )}
            </div>
          ))}
        </div>
      </div>

      {/* 인벤토리 - 소유자만 */}
      {isOwner && (
        <>
          {/* 장비 인벤토리 */}
          <div className="card mb-12">
            <h2>인벤토리 - 장비 ({unequippedItems.length})</h2>
            {unequippedItems.length === 0 && (
              <p style={{ color: '#999', fontSize: '0.9rem' }}>미장착 장비가 없습니다</p>
            )}
            {TYPE_GROUPS.map(group => {
              const items = sortedUnequipped.filter(eq => group.types.includes(eq.type));
              if (items.length === 0) return null;
              return (
                <div key={group.label} style={{ marginBottom: 12 }}>
                  <div style={{
                    fontSize: '0.8rem', fontWeight: 'bold', color: '#6c5ce7',
                    padding: '4px 8px', background: 'rgba(108,92,231,0.1)',
                    borderRadius: 6, marginBottom: 6,
                  }}>
                    {group.label} ({items.length})
                  </div>
                  {items.map(eq => (
                    <div key={eq.id} style={{
                      padding: '6px 8px', borderBottom: '1px solid rgba(255,255,255,0.05)',
                      display: 'flex', justifyContent: 'space-between', alignItems: 'center',
                    }}>
                      <div style={{ flex: 1, minWidth: 0 }}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: 4, flexWrap: 'wrap' }}>
                          <span style={{ fontSize: '0.6rem', padding: '1px 4px', borderRadius: 3,
                            background: `var(--grade-${eq.grade.toLowerCase()})`, color: '#fff' }}>
                            {GRADE_LABEL[eq.grade]}
                          </span>
                          <span className={GRADE_CLASS[eq.grade]} style={{ fontSize: '0.8rem' }}>
                            {TYPE_EMOJI[eq.type] || ''} {eq.name}
                          </span>
                          {eq.twoHanded && <span style={{ fontSize: '0.6rem', color: '#9b59b6' }}>(양손)</span>}
                          {eq.enhanceLevel > 0 && <span style={{ fontSize: '0.6rem', color: '#ffd700' }}>+{eq.enhanceLevel}</span>}
                        </div>
                        <div style={{ fontSize: '0.7rem', color: '#999', marginTop: 2 }}>
                          {eq.attackBonus > 0 && <span style={{ color: '#e74c3c' }}>ATK+{eq.attackBonus} </span>}
                          {eq.defenseBonus > 0 && <span style={{ color: '#3498db' }}>DEF+{eq.defenseBonus} </span>}
                          {eq.type === 'WEAPON' && eq.baseDamageMax > 0 && (
                            <span style={{ color: '#f39c12' }}>DMG:{eq.baseDamageMin}-{eq.baseDamageMax} </span>
                          )}
                          {(() => {
                            const s: string[] = [];
                            if (eq.bonusStrength > 0) s.push(`STR+${eq.bonusStrength}`);
                            if (eq.bonusDexterity > 0) s.push(`DEX+${eq.bonusDexterity}`);
                            if (eq.bonusConstitution > 0) s.push(`CON+${eq.bonusConstitution}`);
                            if (eq.bonusIntelligence > 0) s.push(`INT+${eq.bonusIntelligence}`);
                            if (eq.bonusWisdom > 0) s.push(`WIS+${eq.bonusWisdom}`);
                            if (eq.bonusCharisma > 0) s.push(`CHA+${eq.bonusCharisma}`);
                            return s.length > 0 ? <span style={{ color: '#2ecc71' }}>{s.join(' ')} </span> : null;
                          })()}
                        </div>
                        <div>{renderEffects(eq)}</div>
                      </div>
                      <div style={{ display: 'flex', gap: 6, flexShrink: 0 }}>
                        <button className="btn-sm btn-green" onClick={() => handleEquip(eq.id)}>장착</button>
                        <button className="btn-sm btn-blue" onClick={() => openEnhance(eq)}>강화</button>
                        <button className="btn-sm" style={{
                          background: '#e67e22', color: '#fff', border: 'none', cursor: 'pointer',
                          padding: '4px 8px', borderRadius: 4, fontSize: '0.7rem',
                        }} onClick={() => handleSell(eq)}>
                          판매 {SELL_PRICE[eq.grade] || 5}G
                        </button>
                      </div>
                    </div>
                  ))}
                </div>
              );
            })}
          </div>

          {/* 물약 인벤토리 */}
          <div className="card mb-12">
            <h2>인벤토리 - 물약 ({unequippedPotions.length})</h2>
            {unequippedPotions.length === 0 && (
              <p style={{ color: '#999', fontSize: '0.9rem' }}>보관 중인 물약이 없습니다</p>
            )}
            {unequippedPotions.map(potion => (
              <div key={potion.id} style={{
                padding: '6px 8px', borderBottom: '1px solid rgba(255,255,255,0.05)',
                display: 'flex', justifyContent: 'space-between', alignItems: 'center',
              }}>
                <div>
                  <span style={{ fontSize: '0.9rem' }}>{POTION_EMOJI[potion.buffType || ''] || '🧪'}</span>
                  <span style={{ fontSize: '0.85rem', color: '#2ecc71', marginLeft: 6 }}>{potion.name}</span>
                  <span style={{ fontSize: '0.75rem', color: '#999', marginLeft: 4 }}>x{potion.quantity}</span>
                </div>
                <div style={{ display: 'flex', gap: 6, flexShrink: 0 }}>
                  <button className="btn-sm btn-green" onClick={() => handleEquipPotion(potion.id)}>장착</button>
                  <button className="btn-sm" style={{
                    background: '#e67e22', color: '#fff', border: 'none', cursor: 'pointer',
                    padding: '4px 8px', borderRadius: 4, fontSize: '0.7rem',
                  }} onClick={() => handleSellPotion(potion)}>
                    판매 {potion.sellPrice}G
                  </button>
                </div>
              </div>
            ))}
          </div>
        </>
      )}

      {error && <p className="error mb-12">{error}</p>}

      {/* 캐릭터 삭제 */}
      {isOwner && (
        <div className="card mb-12" style={{ borderColor: 'rgba(231,76,60,0.3)' }}>
          <button className="btn-full btn-red" onClick={async () => {
            if (!confirm(`정말 "${char.name}" 캐릭터를 삭제하시겠습니까?\n모든 장비, 아이템, 전투 기록이 삭제됩니다.`)) return;
            try {
              await api.deleteCharacter(myId);
              localStorage.removeItem('myCharId');
              navigate('/create');
            } catch (e: unknown) {
              setError(e instanceof Error ? e.message : '삭제 실패');
            }
          }}>
            캐릭터 삭제
          </button>
        </div>
      )}

      {/* ===== 장비 비교 모달 ===== */}
      {compareModal && (() => {
        const { newEquip, oldEquip } = compareModal;
        const renderStat = (eq: Equipment) => (
          <div style={{ flex: 1, padding: 8, background: 'rgba(255,255,255,0.03)', borderRadius: 6 }}>
            <div className={GRADE_CLASS[eq.grade]} style={{ fontWeight: 'bold', fontSize: '0.9rem', marginBottom: 4 }}>
              {eq.enhanceLevel > 0 && `+${eq.enhanceLevel} `}{eq.name}
            </div>
            <div style={{ fontSize: '0.75rem', color: '#999', marginBottom: 4 }}>{GRADE_LABEL[eq.grade]}</div>
            {eq.attackBonus > 0 && <div style={{ fontSize: '0.8rem', color: '#e74c3c' }}>ATK +{eq.attackBonus}</div>}
            {eq.defenseBonus > 0 && <div style={{ fontSize: '0.8rem', color: '#3498db' }}>DEF +{eq.defenseBonus}</div>}
            {eq.type === 'WEAPON' && eq.baseDamageMax > 0 && (
              <div style={{ fontSize: '0.8rem', color: '#f39c12' }}>DMG {eq.baseDamageMin}-{eq.baseDamageMax}</div>
            )}
            <div style={{ fontSize: '0.75rem', color: '#aaa', marginTop: 4 }}>
              점수: {equipScore(eq).toFixed(1)}
            </div>
          </div>
        );
        return (
          <div className="modal-overlay" onClick={() => setCompareModal(null)}>
            <div className="modal-content" onClick={e => e.stopPropagation()} style={{ maxWidth: 360 }}>
              <h3 style={{ textAlign: 'center', marginBottom: 8, color: '#e74c3c' }}>
                현재 장비보다 안좋은 장비입니다
              </h3>
              <p style={{ textAlign: 'center', fontSize: '0.8rem', color: '#999', marginBottom: 12 }}>
                그래도 장착하시겠습니까?
              </p>
              <div style={{ display: 'flex', gap: 8, marginBottom: 12 }}>
                <div style={{ flex: 1, textAlign: 'center' }}>
                  <div style={{ fontSize: '0.7rem', color: '#2ecc71', marginBottom: 4 }}>현재 장비</div>
                  {renderStat(oldEquip)}
                </div>
                <div style={{ display: 'flex', alignItems: 'center', fontSize: '1.2rem', color: '#666' }}>→</div>
                <div style={{ flex: 1, textAlign: 'center' }}>
                  <div style={{ fontSize: '0.7rem', color: '#e74c3c', marginBottom: 4 }}>장착할 장비</div>
                  {renderStat(newEquip)}
                </div>
              </div>
              <div style={{ display: 'flex', gap: 8 }}>
                <button className="btn-full" style={{ background: '#555' }}
                  onClick={() => setCompareModal(null)}>취소</button>
                <button className="btn-full btn-red"
                  onClick={() => { setCompareModal(null); doEquip(newEquip.id); }}>장착</button>
              </div>
            </div>
          </div>
        );
      })()}

      {/* ===== 강화 모달 ===== */}
      {enhanceModalOpen && enhanceTarget && enhanceInfo && (
        <div className="modal-overlay" onClick={enhanceBroken ? undefined : closeEnhanceModal}>
          <div className="modal-content" onClick={e => e.stopPropagation()}
            style={enhanceBroken ? { animation: 'enhance-shake 0.5s ease-in-out' } : undefined}>

            {/* 파괴 이펙트 오버레이 */}
            {enhanceBroken && (
              <div style={{
                position: 'absolute', inset: 0, zIndex: 10, borderRadius: 12, overflow: 'hidden',
                display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center',
                background: 'rgba(0,0,0,0.85)',
                animation: 'enhance-break-in 0.3s ease-out',
              }}>
                <div style={{
                  fontSize: '3rem',
                  animation: 'enhance-crack 0.6s ease-out 0.2s both',
                }}>💥</div>
                <div style={{
                  fontSize: '1.3rem', fontWeight: 'bold', color: '#e74c3c', marginTop: 12,
                  animation: 'enhance-crack 0.6s ease-out 0.4s both',
                  textShadow: '0 0 20px rgba(231,76,60,0.8)',
                }}>장비가 파괴되었습니다!</div>
                <div style={{
                  fontSize: '0.9rem', color: '#999', marginTop: 8,
                  animation: 'enhance-crack 0.6s ease-out 0.6s both',
                }}>{enhanceTarget.name}</div>
              </div>
            )}

            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
              <h2 style={{ margin: 0 }}>🔨 장비 강화</h2>
              <button onClick={closeEnhanceModal} style={{
                background: 'none', border: 'none', color: '#999', fontSize: '1.5rem', cursor: 'pointer',
              }}>✕</button>
            </div>

            {/* 장비 정보 */}
            <div style={{ textAlign: 'center', marginBottom: 16, padding: '12px', background: 'rgba(255,255,255,0.03)', borderRadius: 8 }}>
              <div style={{ fontSize: '0.7rem', color: '#999' }}>
                {TYPE_EMOJI[enhanceTarget.type]} {TYPE_LABEL[enhanceTarget.type] || enhanceTarget.type}
              </div>
              <div className={GRADE_CLASS[enhanceTarget.grade]} style={{ fontSize: '1.1rem', fontWeight: 'bold', marginTop: 4 }}>
                {enhanceTarget.name}
              </div>
              <div style={{ fontSize: '0.8rem', color: '#999', marginTop: 4 }}>
                {enhanceTarget.attackBonus > 0 && <span style={{ color: '#e74c3c' }}>ATK+{enhanceTarget.attackBonus} </span>}
                {enhanceTarget.defenseBonus > 0 && <span style={{ color: '#3498db' }}>DEF+{enhanceTarget.defenseBonus} </span>}
                {enhanceTarget.type === 'WEAPON' && enhanceTarget.baseDamageMax > 0 && (
                  <span style={{ color: '#f39c12' }}>DMG:{enhanceTarget.baseDamageMin}-{enhanceTarget.baseDamageMax} </span>
                )}
                {(() => {
                  const s: string[] = [];
                  if (enhanceTarget.bonusStrength > 0) s.push(`STR+${enhanceTarget.bonusStrength}`);
                  if (enhanceTarget.bonusDexterity > 0) s.push(`DEX+${enhanceTarget.bonusDexterity}`);
                  if (enhanceTarget.bonusConstitution > 0) s.push(`CON+${enhanceTarget.bonusConstitution}`);
                  if (enhanceTarget.bonusIntelligence > 0) s.push(`INT+${enhanceTarget.bonusIntelligence}`);
                  if (enhanceTarget.bonusWisdom > 0) s.push(`WIS+${enhanceTarget.bonusWisdom}`);
                  if (enhanceTarget.bonusCharisma > 0) s.push(`CHA+${enhanceTarget.bonusCharisma}`);
                  return s.length > 0 ? <span style={{ color: '#2ecc71' }}>{s.join(' ')}</span> : null;
                })()}
              </div>
              {/* 현재 효과 표시 */}
              <div style={{ marginTop: 8 }}>{renderEffects(enhanceTarget)}</div>
            </div>

            {!effectSelectionMode ? (
              <>
                {/* 강화 정보 */}
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 8, marginBottom: 16 }}>
                  <div style={{ background: 'rgba(255,255,255,0.05)', borderRadius: 6, padding: 10, textAlign: 'center' }}>
                    <div style={{ fontSize: '0.7rem', color: '#999' }}>비용</div>
                    <div style={{ fontSize: '1.1rem', color: '#ffd700', fontWeight: 'bold' }}>{enhanceInfo.cost}G</div>
                  </div>
                  <div style={{ background: 'rgba(255,255,255,0.05)', borderRadius: 6, padding: 10, textAlign: 'center' }}>
                    <div style={{ fontSize: '0.7rem', color: '#999' }}>성공률</div>
                    <div style={{ fontSize: '1.1rem', color: '#2ecc71', fontWeight: 'bold' }}>{enhanceInfo.successRate}%</div>
                  </div>
                  <div style={{ background: 'rgba(255,255,255,0.05)', borderRadius: 6, padding: 10, textAlign: 'center' }}>
                    <div style={{ fontSize: '0.7rem', color: '#999' }}>파괴 확률</div>
                    <div style={{ fontSize: '1.1rem', color: enhanceInfo.breakChance > 0 ? '#e74c3c' : '#2ecc71', fontWeight: 'bold' }}>
                      {enhanceInfo.breakChance}%
                    </div>
                  </div>
                  <div style={{ background: 'rgba(255,255,255,0.05)', borderRadius: 6, padding: 10, textAlign: 'center' }}>
                    <div style={{ fontSize: '0.7rem', color: '#999' }}>다음 스탯 보너스</div>
                    <div style={{ fontSize: '1.1rem', color: '#3498db', fontWeight: 'bold' }}>+{enhanceInfo.nextStatBonus}</div>
                  </div>
                </div>

                {/* 강화 효과 슬롯 정보 */}
                {(enhanceInfo.maxEnhanceEffects > 0 || (enhanceInfo.currentEnhanceEffects && enhanceInfo.currentEnhanceEffects.length > 0)) && (
                  <div style={{ marginBottom: 12, padding: '8px 12px', background: 'rgba(231,76,60,0.1)', borderRadius: 6 }}>
                    <div style={{ fontSize: '0.75rem', color: '#e74c3c', fontWeight: 'bold', marginBottom: 4 }}>
                      강화 효과 ({(enhanceInfo.currentEnhanceEffects || []).length}/{enhanceInfo.maxEnhanceEffects})
                    </div>
                    {(enhanceInfo.currentEnhanceEffects || []).map((e, i) => (
                      <div key={i} style={{ fontSize: '0.7rem', color: '#e74c3c' }}>
                        [{EFFECT_NAMES[e.effect] || e.effectName} {e.effectChance}%]
                      </div>
                    ))}
                  </div>
                )}

                <button className="btn-full btn-gold" onClick={handleEnhance}
                  disabled={!char || char.gold < enhanceInfo.cost}
                  style={{ padding: '12px', fontSize: '1rem' }}>
                  🔨 강화하기 ({enhanceInfo.cost}G)
                </button>
              </>
            ) : (
              /* 효과 선택 모드 */
              <div>
                <div style={{ fontSize: '0.85rem', color: '#f39c12', fontWeight: 'bold', marginBottom: 8 }}>
                  강화 효과를 선택하세요 ({selectedEffects.size}/{maxEnhanceEffects})
                </div>
                <div style={{ fontSize: '0.7rem', color: '#999', marginBottom: 12 }}>
                  기존 효과와 새 후보 중에서 최대 {maxEnhanceEffects}개를 선택할 수 있습니다.
                </div>

                {/* 기존 강화 효과 */}
                {currentEnhanceEffects.length > 0 && (
                  <div style={{ marginBottom: 8 }}>
                    <div style={{ fontSize: '0.7rem', color: '#999', marginBottom: 4 }}>기존 효과</div>
                    {currentEnhanceEffects.map((e, i) => (
                      <div key={`current-${i}`}
                        onClick={() => toggleEffectSelection(e.effect)}
                        style={{
                          padding: '8px 12px', marginBottom: 4, borderRadius: 6, cursor: 'pointer',
                          background: selectedEffects.has(e.effect) ? 'rgba(231,76,60,0.2)' : 'rgba(255,255,255,0.05)',
                          border: selectedEffects.has(e.effect) ? '1px solid #e74c3c' : '1px solid transparent',
                        }}>
                        <div style={{ fontSize: '0.8rem', color: '#e74c3c', fontWeight: 'bold' }}>
                          {selectedEffects.has(e.effect) ? '✓ ' : ''}{EFFECT_NAMES[e.effect] || e.effectName}
                        </div>
                        <div style={{ fontSize: '0.65rem', color: '#999' }}>
                          발동 {e.effectChance}% | 수치 {e.effectValue}
                        </div>
                      </div>
                    ))}
                  </div>
                )}

                {/* 새 후보 효과 */}
                <div style={{ marginBottom: 12 }}>
                  <div style={{ fontSize: '0.7rem', color: '#999', marginBottom: 4 }}>새 후보 효과</div>
                  {candidateEffects.map((e, i) => (
                    <div key={`candidate-${i}`}
                      onClick={() => toggleEffectSelection(e.effect)}
                      style={{
                        padding: '8px 12px', marginBottom: 4, borderRadius: 6, cursor: 'pointer',
                        background: selectedEffects.has(e.effect) ? 'rgba(46,204,113,0.2)' : 'rgba(255,255,255,0.05)',
                        border: selectedEffects.has(e.effect) ? '1px solid #2ecc71' : '1px solid transparent',
                      }}>
                      <div style={{ fontSize: '0.8rem', color: '#2ecc71', fontWeight: 'bold' }}>
                        {selectedEffects.has(e.effect) ? '✓ ' : ''}{EFFECT_NAMES[e.effect] || e.effectName}
                        <span style={{ fontSize: '0.65rem', color: '#f39c12', marginLeft: 4 }}>NEW</span>
                      </div>
                      <div style={{ fontSize: '0.65rem', color: '#999' }}>
                        발동 {e.effectChance}% | 수치 {e.effectValue}
                      </div>
                    </div>
                  ))}
                </div>

                <button className="btn-full btn-gold" onClick={confirmEffectSelection}
                  disabled={selectedEffects.size === 0}
                  style={{ padding: '12px', fontSize: '1rem' }}>
                  효과 확정 ({selectedEffects.size}/{maxEnhanceEffects})
                </button>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
