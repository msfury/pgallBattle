import { useEffect, useState } from 'react';
import { CLASS_COLOR } from '../data/classes';
import SpriteAvatar from './SpriteAvatar';

interface BattleArenaProps {
  attackerAvatar: string | null;
  defenderAvatar: string | null;
  attackerClass: string | null;
  defenderClass: string | null;
  attackerName: string;
  defenderName: string;
  attackerMaxHp: number;
  defenderMaxHp: number;
  currentLog: string | null;
  logIndex: number;
}

type AnimationType = 'idle' | 'walk' | 'attack' | 'hit' | 'death';

function detectAnimation(log: string, name: string): { anim: AnimationType; targetAnim: AnimationType } {
  const isAttacker = log.includes(`${name}의`) || log.startsWith(`[${name}]`) || log.includes(`${name} ->`);

  if (log.includes('크리티컬') || log.includes('명중') || log.includes('데미지') || log.includes('공격')) {
    return isAttacker ? { anim: 'attack', targetAnim: 'hit' } : { anim: 'hit', targetAnim: 'attack' };
  }
  if (log.includes('빗나감') || log.includes('회피') || log.includes('차단')) {
    return { anim: 'attack', targetAnim: 'idle' };
  }
  if (log.includes('쓰러') || log.includes('패배')) {
    return { anim: 'idle', targetAnim: 'death' };
  }
  return { anim: 'idle', targetAnim: 'idle' };
}

export default function BattleArena({
  attackerAvatar, defenderAvatar, attackerClass, defenderClass,
  attackerName, defenderName, attackerMaxHp, defenderMaxHp,
  currentLog, logIndex,
}: BattleArenaProps) {
  const [attackerHp, setAttackerHp] = useState(attackerMaxHp);
  const [defenderHp, setDefenderHp] = useState(defenderMaxHp);
  const [leftAnim, setLeftAnim] = useState<AnimationType>('idle');
  const [rightAnim, setRightAnim] = useState<AnimationType>('idle');

  useEffect(() => {
    if (!currentLog) return;

    // Parse HP from log
    const hpMatch = currentLog.match(/남은\s*HP[:\s]*(-?\d+)/);
    if (hpMatch) {
      const hp = Math.max(0, Number(hpMatch[1]));
      if (currentLog.includes(defenderName)) setDefenderHp(hp);
      else if (currentLog.includes(attackerName)) setAttackerHp(hp);
    }

    if (currentLog.includes('패배') || currentLog.includes('쓰러')) {
      if (currentLog.includes(attackerName)) setAttackerHp(0);
      if (currentLog.includes(defenderName)) setDefenderHp(0);
    }

    // Determine who is acting
    const attackerActing = currentLog.includes(`${attackerName}`) &&
      (currentLog.includes(`${attackerName} ->`) || currentLog.includes(`${attackerName}의`));

    if (currentLog.includes('명중') || currentLog.includes('크리티컬') || currentLog.includes('데미지')) {
      if (attackerActing) {
        setLeftAnim('attack');
        setRightAnim('hit');
      } else {
        setRightAnim('attack');
        setLeftAnim('hit');
      }
    } else if (currentLog.includes('빗나감') || currentLog.includes('차단')) {
      if (attackerActing) setLeftAnim('attack');
      else setRightAnim('attack');
    } else if (currentLog.includes('승리')) {
      // winner stays idle, loser death
      if (currentLog.includes(attackerName)) setDefenderAnim('death');
      else setAttackerAnim('death');
    }

    const timer = setTimeout(() => {
      setLeftAnim('idle');
      setRightAnim('idle');
    }, 500);
    return () => clearTimeout(timer);
  }, [currentLog, logIndex]);

  // Helper to safely set anims
  function setAttackerAnim(a: AnimationType) { setLeftAnim(a); }
  function setDefenderAnim(a: AnimationType) { setRightAnim(a); }

  const renderFighter = (
    avatar: string | null,
    charClass: string | null,
    name: string,
    hp: number,
    maxHp: number,
    anim: AnimationType,
    flip: boolean,
  ) => {
    const hpPct = Math.max(0, Math.min(100, (hp / maxHp) * 100));
    const hpColor = hpPct > 50 ? '#2ecc71' : hpPct > 25 ? '#f39c12' : '#e74c3c';

    return (
      <div className="arena-fighter">
        <SpriteAvatar avatarId={avatar} animation={anim} scale={0.8} flip={flip} />
        <div className="arena-name">{name}</div>
        {charClass && (
          <div className="arena-class-badge"
            style={{ background: `${CLASS_COLOR[charClass] || '#666'}33`, color: CLASS_COLOR[charClass] || '#999' }}>
            {charClass}
          </div>
        )}
        <div className="hp-bar">
          <div className="hp-fill" style={{ width: `${hpPct}%`, background: hpColor }} />
        </div>
        <div className="hp-text">{hp}/{maxHp}</div>
      </div>
    );
  };

  return (
    <div className="battle-arena" style={{ background: 'var(--bg-card)', borderRadius: 12, marginBottom: 12 }}>
      {renderFighter(attackerAvatar, attackerClass, attackerName, attackerHp, attackerMaxHp, leftAnim, false)}
      <div className="arena-vs">VS</div>
      {renderFighter(defenderAvatar, defenderClass, defenderName, defenderHp, defenderMaxHp, rightAnim, true)}
    </div>
  );
}
