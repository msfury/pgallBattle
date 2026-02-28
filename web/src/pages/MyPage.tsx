import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { api, type Character, type Equipment } from '../api/client';
import { CLASS_EMOJI, CLASS_COLOR } from '../data/classes';
import SpriteAvatar from '../components/SpriteAvatar';

const GRADE_CLASS: Record<string, string> = {
  COMMON: 'grade-common', UNCOMMON: 'grade-uncommon', RARE: 'grade-rare',
  EPIC: 'grade-epic', LEGENDARY: 'grade-legendary',
};

const EFFECT_NAMES: Record<string, string> = {
  // 무기 효과
  FIRE_DAMAGE: '화염 공격', ICE_DAMAGE: '빙결 공격', LIGHTNING_DAMAGE: '번개 공격',
  HOLY_DAMAGE: '신성 공격', DARK_DAMAGE: '암흑 공격', ACID_DAMAGE: '산성 공격',
  ARMOR_PENETRATION: '관통', BLEEDING: '출혈', LIFE_STEAL: '흡혈', DOUBLE_ATTACK: '더블 어택',
  CRITICAL_BOOST: '크리티컬 강화', STUN_STRIKE: '기절 타격', KNOCKBACK: '넉백', VORPAL: '참수',
  DEBUFF_ATK_DOWN: '공격력 감소', DEBUFF_DEF_DOWN: '방어력 감소', SLOW: '속도 감소',
  SILENCE: '침묵', DISARM: '무장 해제', EXECUTE: '처형',
  // 방어구 효과
  BLOCK_CHANCE: '공격 차단', MAGIC_RESISTANCE: '마법 저항', THORNS: '가시', HP_REGEN: 'HP 재생',
  DAMAGE_REDUCTION: '피해 감소', DODGE_BOOST: '회피 증가', FIRE_RESISTANCE: '화염 저항',
  ICE_RESISTANCE: '빙결 저항', LIGHTNING_RESISTANCE: '번개 저항', POISON_RESISTANCE: '독 저항',
  STUN_RESISTANCE: '기절 저항', REFLECT_MAGIC: '마법 반사', SECOND_WIND: '재기',
  HEAVY_ARMOR: '중갑', ENDURANCE: '인내', PERSEVERANCE: '불굴', IRON_SKIN: '철피',
  HEALING_AURA: '치유 오라', ABSORB_SHIELD: '흡수 보호막', FORTIFY: '강화',
  // 악세사리 효과
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

const SLOT_LAYOUT = [
  { type: 'WEAPON', label: '무기' },
  { type: 'HELMET', label: '투구' },
  { type: 'ARMOR', label: '갑옷' },
  { type: 'GLOVES', label: '장갑' },
  { type: 'SHOES', label: '신발' },
  { type: 'EARRING', label: '귀걸이 1' },
  { type: 'EARRING', label: '귀걸이 2' },
  { type: 'RING', label: '반지 1' },
  { type: 'RING', label: '반지 2' },
];

export default function MyPage() {
  const { id } = useParams<{ id: string }>();
  const myId = Number(id);
  const navigate = useNavigate();

  const [char, setChar] = useState<Character | null>(null);
  const [error, setError] = useState('');

  const loadChar = () => api.getCharacter(myId).then(setChar).catch(e => {
    setError(e instanceof Error ? e.message : '로딩 실패');
  });

  useEffect(() => { loadChar(); }, [myId]);

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

  if (!char) return <div className="text-center mt-16">{error || '로딩 중...'}</div>;

  const equippedItems = char.equipments.filter(e => e.equipped);
  const unequippedItems = char.equipments.filter(e => !e.equipped);

  const slotItems: (Equipment | null)[] = [];
  const usedIds = new Set<number>();
  for (const slot of SLOT_LAYOUT) {
    const item = equippedItems.find(e => e.type === slot.type && !usedIds.has(e.id));
    if (item) usedIds.add(item.id);
    slotItems.push(item || null);
  }

  const stats = [
    { label: 'STR 힘', value: char.strength }, { label: 'DEX 민첩', value: char.dexterity },
    { label: 'CON 체력', value: char.constitution }, { label: 'INT 지능', value: char.intelligence },
    { label: 'WIS 지혜', value: char.wisdom }, { label: 'CHA 매력', value: char.charisma },
  ];

  return (
    <div>
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
      <div className="flex-row mb-12">
        <button className="btn-gold" onClick={() => navigate(`/shop/${myId}`)}>🏪 상점</button>
        <button className="btn-blue" onClick={() => navigate(`/gacha/${myId}`)}>🎰 가챠</button>
      </div>

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

      {/* 장착 슬롯 */}
      <div className="card mb-12">
        <h2>장착 장비</h2>
        <div className="equip-grid">
          {SLOT_LAYOUT.map((slot, i) => {
            const item = slotItems[i];
            return (
              <div key={`${slot.type}-${i}`} className={`equip-slot ${item ? 'filled' : ''}`}>
                <div className="slot-label">{TYPE_EMOJI[slot.type]} {slot.label}</div>
                {item ? (
                  <div>
                    <div className={GRADE_CLASS[item.grade]} style={{ fontSize: '0.8rem', fontWeight: 'bold' }}>
                      {item.name}
                    </div>
                    <div style={{ fontSize: '0.65rem', color: '#999', marginTop: 2 }}>
                      {item.attackBonus > 0 && <span style={{ color: '#e74c3c' }}>ATK+{item.attackBonus} </span>}
                      {item.defenseBonus > 0 && <span style={{ color: '#3498db' }}>DEF+{item.defenseBonus} </span>}
                      {item.type === 'WEAPON' && item.baseDamageMax > 0 && (
                        <span style={{ color: '#f39c12' }}>DMG:{item.baseDamageMin}-{item.baseDamageMax} </span>
                      )}
                    </div>
                    {item.effect && (
                      <div style={{ fontSize: '0.6rem', color: '#f39c12' }}>
                        [{EFFECT_NAMES[item.effect] || item.effect} {item.effectChance}%]
                      </div>
                    )}
                    <button className="btn-sm btn-red" style={{ marginTop: 4 }}
                      onClick={() => handleUnequip(item.id)}>해제</button>
                  </div>
                ) : (
                  <div className="slot-empty">비어있음</div>
                )}
              </div>
            );
          })}
        </div>
      </div>

      {/* 인벤토리 */}
      <div className="card mb-12">
        <h2>인벤토리 ({unequippedItems.length})</h2>
        {unequippedItems.length === 0 && (
          <p style={{ color: '#999', fontSize: '0.9rem' }}>미장착 장비가 없습니다</p>
        )}
        {unequippedItems.map(eq => (
          <div key={eq.id} style={{
            padding: '8px 0', borderBottom: '1px solid rgba(255,255,255,0.05)',
            display: 'flex', justifyContent: 'space-between', alignItems: 'center',
          }}>
            <div>
              <span className={GRADE_CLASS[eq.grade]}>{TYPE_EMOJI[eq.type] || ''} {eq.name}</span>
              {eq.twoHanded && <span style={{ fontSize: '0.65rem', color: '#999', marginLeft: 4 }}>(양손)</span>}
              <div style={{ fontSize: '0.75rem', color: '#999', marginTop: 2 }}>
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
            <button className="btn-sm btn-green" onClick={() => handleEquip(eq.id)}>장착</button>
          </div>
        ))}
      </div>

      {error && <p className="error mb-12">{error}</p>}
    </div>
  );
}
