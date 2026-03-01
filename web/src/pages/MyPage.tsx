import { useEffect, useState, useCallback } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { api, type Character, type Equipment, type InventoryItem, type EnhanceResult } from '../api/client';
import { CLASS_EMOJI, CLASS_COLOR } from '../data/classes';
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

// 인벤토리 타입 그룹
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
  const [enhanceTarget, setEnhanceTarget] = useState<Equipment | null>(null);
  const [enhanceInfo, setEnhanceInfo] = useState<EnhanceResult | null>(null);

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

  const handleEquip = async (equipId: number) => {
    try {
      setError('');
      await api.equipItem(myId, equipId);
      await loadChar();
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : '장착 실패');
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
    // 레어급 이상만 확인 얼럿
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

  // 물약 핸들러
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

  // 강화 핸들러
  const openEnhance = async (eq: Equipment) => {
    try {
      const info = await api.enhanceInfo(myId, eq.id);
      setEnhanceTarget(eq);
      setEnhanceInfo(info);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : '강화 정보 조회 실패');
    }
  };

  const handleEnhance = async () => {
    if (!enhanceTarget) return;
    try {
      setError('');
      const result = await api.enhance(myId, enhanceTarget.id);
      if (result.broken) {
        setToast(result.message);
        setEnhanceTarget(null);
        setEnhanceInfo(null);
      } else if (result.success) {
        setToast(result.message);
        // 강화 후 최신 캐릭터 & 무기 정보 갱신
        const refreshed = await api.getCharacter(myId);
        const updatedWeapon = refreshed.equipments.find(e => e.id === enhanceTarget.id);
        if (updatedWeapon) setEnhanceTarget(updatedWeapon);
        const info = await api.enhanceInfo(myId, enhanceTarget.id);
        setEnhanceInfo(info);
      } else {
        setToast(result.message);
      }
      await loadChar();
      setTimeout(() => setToast(''), 3000);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : '강화 실패');
    }
  };

  if (!char) return <div className="text-center mt-16">{error || '로딩 중...'}</div>;

  const equippedItems = char.equipments.filter(e => e.equipped);
  const unequippedItems = char.equipments.filter(e => !e.equipped);

  // 양손무기 감지
  const equippedWeapons = equippedItems.filter(e => e.type === 'WEAPON');
  const twoHandedWeapon = equippedWeapons.find(e => e.twoHanded);

  const slotItems: (Equipment | null)[] = [];
  const usedIds = new Set<number>();
  for (const slot of SLOT_LAYOUT) {
    if (slot.type === 'WEAPON' && slot.label === '무기 2' && twoHandedWeapon) {
      // 양손무기: 무기 2 슬롯에도 같은 무기 표시
      slotItems.push(twoHandedWeapon);
    } else {
      const item = equippedItems.find(e => e.type === slot.type && !usedIds.has(e.id));
      if (item) usedIds.add(item.id);
      slotItems.push(item || null);
    }
  }

  // 물약 슬롯 데이터
  const equippedPotions = (char.potions || []).filter(p => p.equipped);
  const unequippedPotions = (char.potions || []).filter(p => !p.equipped);
  const potionSlots: (InventoryItem | null)[] = [];
  for (let i = 0; i < 5; i++) {
    potionSlots.push(equippedPotions[i] || null);
  }

  // 인벤토리를 타입 그룹별 + 등급순으로 정렬
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

  const renderEquipItem = (eq: Equipment) => (
    <div style={{ fontSize: '0.65rem', color: '#999', marginTop: 2 }}>
      {eq.attackBonus > 0 && <span style={{ color: '#e74c3c' }}>ATK+{eq.attackBonus} </span>}
      {eq.defenseBonus > 0 && <span style={{ color: '#3498db' }}>DEF+{eq.defenseBonus} </span>}
      {eq.type === 'WEAPON' && eq.baseDamageMax > 0 && (
        <span style={{ color: '#f39c12' }}>DMG:{eq.baseDamageMin}-{eq.baseDamageMax} </span>
      )}
      {eq.enhanceLevel > 0 && (
        <span style={{ color: '#ffd700' }}>+{eq.enhanceLevel} </span>
      )}
    </div>
  );

  return (
    <div>
      {/* 일급 토스트 */}
      {toast && (
        <div style={{
          position: 'fixed', top: 16, left: '50%', transform: 'translateX(-50%)',
          background: 'linear-gradient(135deg, #f39c12, #e67e22)', color: '#fff',
          padding: '12px 24px', borderRadius: 12, fontWeight: 'bold', fontSize: '0.95rem',
          zIndex: 9999, boxShadow: '0 4px 20px rgba(243,156,18,0.4)',
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
                <span style={{ color: CLASS_COLOR[char.characterClass] || '#999', marginLeft: 6 }}>
                  {CLASS_EMOJI[char.characterClass]} {char.classKoreanName}
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
                    {renderEquipItem(item)}
                    {item.effect && (
                      <div style={{ fontSize: '0.6rem', color: '#f39c12' }}>
                        [{EFFECT_NAMES[item.effect] || item.effect} {item.effectChance}%]
                      </div>
                    )}
                    {isTwoHandedSlot2 && (
                      <div style={{ fontSize: '0.6rem', color: '#9b59b6' }}>(양손무기)</div>
                    )}
                    {isOwner && !isTwoHandedSlot2 && (
                      <div style={{ display: 'flex', gap: 3, justifyContent: 'center', marginTop: 4 }}>
                        <button className="btn-sm btn-red"
                          onClick={() => handleUnequip(item.id)}>해제</button>
                        {item.type === 'WEAPON' && (
                          <button className="btn-sm btn-blue"
                            onClick={() => openEnhance(item)}>강화</button>
                        )}
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

      {/* 강화 패널 */}
      {enhanceTarget && enhanceInfo && (
        <div className="card mb-12" style={{ border: '1px solid rgba(243,156,18,0.5)', background: 'rgba(243,156,18,0.05)' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 }}>
            <h2 style={{ margin: 0 }}>🔨 무기 강화</h2>
            <button className="btn-sm" style={{ background: '#555', color: '#fff', border: 'none', cursor: 'pointer', borderRadius: 4 }}
              onClick={() => { setEnhanceTarget(null); setEnhanceInfo(null); }}>닫기</button>
          </div>
          <div style={{ textAlign: 'center', marginBottom: 12 }}>
            <div className={GRADE_CLASS[enhanceTarget.grade]} style={{ fontSize: '1.1rem', fontWeight: 'bold' }}>
              {enhanceTarget.name}
            </div>
            <div style={{ fontSize: '0.8rem', color: '#999', marginTop: 4 }}>
              ATK+{enhanceTarget.attackBonus} | DMG:{enhanceTarget.baseDamageMin}-{enhanceTarget.baseDamageMax}
            </div>
          </div>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 8, marginBottom: 12 }}>
            <div style={{ background: 'rgba(255,255,255,0.05)', borderRadius: 6, padding: 8, textAlign: 'center' }}>
              <div style={{ fontSize: '0.7rem', color: '#999' }}>비용</div>
              <div style={{ fontSize: '1rem', color: '#ffd700', fontWeight: 'bold' }}>{enhanceInfo.cost}G</div>
            </div>
            <div style={{ background: 'rgba(255,255,255,0.05)', borderRadius: 6, padding: 8, textAlign: 'center' }}>
              <div style={{ fontSize: '0.7rem', color: '#999' }}>성공률</div>
              <div style={{ fontSize: '1rem', color: '#2ecc71', fontWeight: 'bold' }}>{enhanceInfo.successRate}%</div>
            </div>
            <div style={{ background: 'rgba(255,255,255,0.05)', borderRadius: 6, padding: 8, textAlign: 'center' }}>
              <div style={{ fontSize: '0.7rem', color: '#999' }}>파괴 확률</div>
              <div style={{ fontSize: '1rem', color: enhanceInfo.breakChance > 0 ? '#e74c3c' : '#2ecc71', fontWeight: 'bold' }}>
                {enhanceInfo.breakChance}%
              </div>
            </div>
            <div style={{ background: 'rgba(255,255,255,0.05)', borderRadius: 6, padding: 8, textAlign: 'center' }}>
              <div style={{ fontSize: '0.7rem', color: '#999' }}>다음 스탯 보너스</div>
              <div style={{ fontSize: '1rem', color: '#3498db', fontWeight: 'bold' }}>+{enhanceInfo.nextStatBonus}</div>
            </div>
          </div>
          <button className="btn-full btn-gold" onClick={handleEnhance}
            disabled={!char || char.gold < enhanceInfo.cost}>
            🔨 강화하기 ({enhanceInfo.cost}G)
          </button>
        </div>
      )}

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
          {/* 장비 인벤토리 (타입별 → 등급순) */}
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
                        <div style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
                          <span style={{ fontSize: '0.6rem', padding: '1px 4px', borderRadius: 3,
                            background: `var(--grade-${eq.grade.toLowerCase()})`, color: '#fff' }}>
                            {GRADE_LABEL[eq.grade]}
                          </span>
                          <span className={GRADE_CLASS[eq.grade]} style={{ fontSize: '0.8rem' }}>
                            {TYPE_EMOJI[eq.type] || ''} {eq.name}
                          </span>
                          {eq.twoHanded && <span style={{ fontSize: '0.6rem', color: '#9b59b6' }}>(양손)</span>}
                        </div>
                        <div style={{ fontSize: '0.7rem', color: '#999', marginTop: 2 }}>
                          {eq.attackBonus > 0 && <span style={{ color: '#e74c3c' }}>ATK+{eq.attackBonus} </span>}
                          {eq.defenseBonus > 0 && <span style={{ color: '#3498db' }}>DEF+{eq.defenseBonus} </span>}
                          {eq.type === 'WEAPON' && eq.baseDamageMax > 0 && (
                            <span style={{ color: '#f39c12' }}>DMG:{eq.baseDamageMin}-{eq.baseDamageMax} </span>
                          )}
                          {eq.effect && (
                            <span style={{ color: '#f39c12' }}>[{EFFECT_NAMES[eq.effect] || eq.effect} {eq.effectChance}%]</span>
                          )}
                        </div>
                      </div>
                      <div style={{ display: 'flex', gap: 6, flexShrink: 0 }}>
                        <button className="btn-sm btn-green" onClick={() => handleEquip(eq.id)}>장착</button>
                        {eq.type === 'WEAPON' && (
                          <button className="btn-sm btn-blue" onClick={() => openEnhance(eq)}>강화</button>
                        )}
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

      {/* 캐릭터 삭제 - 소유자만 */}
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
    </div>
  );
}
