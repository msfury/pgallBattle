import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { api, type Character, type Equipment } from '../api/client';

const GRADE_CLASS: Record<string, string> = {
  COMMON: 'grade-common', UNCOMMON: 'grade-uncommon', RARE: 'grade-rare',
  EPIC: 'grade-epic', LEGENDARY: 'grade-legendary',
};

const GRADE_LABEL: Record<string, string> = {
  COMMON: '커먼', UNCOMMON: '언커먼', RARE: '레어', EPIC: '에픽', LEGENDARY: '레전더리',
};

const EFFECT_NAMES: Record<string, string> = {
  DOUBLE_ATTACK: '더블 어택', DEBUFF_ATK_DOWN: '공격력 감소', DEBUFF_DEF_DOWN: '방어력 감소',
  BLOCK_CHANCE: '공격 차단', POISON: '독', STUN: '기절', LIFE_STEAL: '흡혈', ACCURACY_UP: '명중률 증가',
};

const TYPE_EMOJI: Record<string, string> = {
  WEAPON: '⚔️', HELMET: '🪖', ARMOR: '🛡️', GLOVES: '🧤', SHOES: '👢', EARRING: '💎', RING: '💍',
};

const SCALING_LABEL: Record<string, string> = {
  STR: '힘', DEX: '민첩', INT: '지능', WIS: '지혜',
};

export default function GachaPage() {
  const { id } = useParams<{ id: string }>();
  const myId = Number(id);
  const navigate = useNavigate();

  const [me, setMe] = useState<Character | null>(null);
  const [result, setResult] = useState<Equipment | null>(null);
  const [pulling, setPulling] = useState(false);
  const [animating, setAnimating] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => { api.getCharacter(myId).then(setMe); }, [myId]);

  const pull = async () => {
    setPulling(true);
    setError('');
    setResult(null);
    setAnimating(true);
    try {
      const eq = await api.gacha(myId);
      setTimeout(() => {
        setResult(eq);
        setAnimating(false);
      }, 1000);
      const updated = await api.getCharacter(myId);
      setMe(updated);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : '가챠 실패');
      setAnimating(false);
    } finally {
      setPulling(false);
    }
  };

  if (!me) return <div className="text-center mt-16">로딩 중...</div>;

  return (
    <div>
      <button className="back-btn" onClick={() => navigate(`/mypage/${myId}`)}>← 뒤로</button>
      <h1>🎰 가챠</h1>
      <p className="text-center mb-16 gold">💰 보유 골드: {me.gold} G</p>

      <div style={{
        textAlign: 'center', padding: 32, marginBottom: 16,
        background: 'var(--bg-card)', borderRadius: 12, minHeight: 180,
        display: 'flex', alignItems: 'center', justifyContent: 'center',
      }}>
        {animating ? (
          <div style={{ fontSize: '3rem', animation: 'spin 0.5s linear infinite' }}>🎲</div>
        ) : result ? (
          <div>
            <div style={{ fontSize: '2.5rem', marginBottom: 8 }}>
              {TYPE_EMOJI[result.type] || '📦'}
            </div>
            <div className={GRADE_CLASS[result.grade]} style={{ fontSize: '1.2rem', fontWeight: 'bold' }}>
              {result.name}
            </div>
            <div style={{ fontSize: '0.85rem', marginTop: 4 }}>
              <span className={GRADE_CLASS[result.grade]}>[{GRADE_LABEL[result.grade]}]</span>
              {result.twoHanded && <span style={{ color: '#999', marginLeft: 6 }}>(양손)</span>}
            </div>
            <div style={{ fontSize: '0.85rem', color: '#999', marginTop: 8 }}>
              {result.attackBonus > 0 && <span style={{ color: '#e74c3c' }}>ATK+{result.attackBonus} </span>}
              {result.defenseBonus > 0 && <span style={{ color: '#3498db' }}>DEF+{result.defenseBonus} </span>}
              {result.type === 'WEAPON' && result.baseDamageMax > 0 && (
                <span style={{ color: '#f39c12' }}>DMG:{result.baseDamageMin}-{result.baseDamageMax} </span>
              )}
            </div>
            {result.type === 'WEAPON' && result.scalingStat && (
              <div style={{ fontSize: '0.8rem', color: '#aaa', marginTop: 4 }}>
                스케일링: {SCALING_LABEL[result.scalingStat] || result.scalingStat}
                {result.weaponCategory && <span> | {result.weaponCategory}</span>}
              </div>
            )}
            {result.effect && (
              <div style={{ fontSize: '0.85rem', color: '#f39c12', marginTop: 4 }}>
                ✨ {EFFECT_NAMES[result.effect] || result.effect} ({result.effectChance}%)
              </div>
            )}
          </div>
        ) : (
          <div style={{ color: '#999' }}>
            <div style={{ fontSize: '2rem', marginBottom: 8 }}>📦</div>
            <p>30 골드로 장비를 뽑아보세요!</p>
            <p style={{ fontSize: '0.8rem', marginTop: 4 }}>무기, 투구, 갑옷, 장갑, 신발, 귀걸이, 반지</p>
          </div>
        )}
      </div>

      <button className="btn-full btn-gold" onClick={pull}
        disabled={pulling || animating || me.gold < 30}>
        {me.gold < 30 ? '골드 부족 (30G 필요)' : '🎰 가챠 뽑기 (30G)'}
      </button>

      {error && <p className="error mt-12">{error}</p>}
    </div>
  );
}
