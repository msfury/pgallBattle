import { useEffect, useState } from 'react';
import { CLASS_COLOR } from '../data/classes';
import type { PotionInfo } from '../api/client';
import SpriteAvatar from './SpriteAvatar';

type AnimationType = 'idle' | 'walk' | 'attack' | 'hit' | 'death';

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
  battleFinished: boolean;
  winnerId: number | null;
  attackerId: number;
  defenderId: number;
  attackerPotions?: PotionInfo[];
  defenderPotions?: PotionInfo[];
}

export default function BattleArena({
  attackerAvatar, defenderAvatar, attackerClass, defenderClass,
  attackerName, defenderName, attackerMaxHp, defenderMaxHp,
  currentLog, logIndex, battleFinished, winnerId, attackerId, defenderId,
  attackerPotions, defenderPotions,
}: BattleArenaProps) {
  const [attackerHp, setAttackerHp] = useState(attackerMaxHp);
  const [defenderHp, setDefenderHp] = useState(defenderMaxHp);
  const [leftAnim, setLeftAnim] = useState<AnimationType>('idle');
  const [rightAnim, setRightAnim] = useState<AnimationType>('idle');
  const [leftPaused, setLeftPaused] = useState(false);
  const [rightPaused, setRightPaused] = useState(false);
  const [leftFrozenFrame, setLeftFrozenFrame] = useState<number | undefined>(undefined);
  const [rightFrozenFrame, setRightFrozenFrame] = useState<number | undefined>(undefined);
  const [usedPotions, setUsedPotions] = useState<Set<string>>(new Set());

  // Battle finished: freeze sprites
  useEffect(() => {
    if (!battleFinished || winnerId === null) return;

    const attackerWon = winnerId === attackerId;

    if (attackerWon) {
      setLeftAnim('idle');
      setLeftPaused(true);
      setLeftFrozenFrame(0);
      setRightAnim('death');
      setRightPaused(true);
      setRightFrozenFrame(2);
    } else {
      setRightAnim('idle');
      setRightPaused(true);
      setRightFrozenFrame(0);
      setLeftAnim('death');
      setLeftPaused(true);
      setLeftFrozenFrame(2);
    }
  }, [battleFinished, winnerId, attackerId, defenderId]);

  // Handle log-based animations (only when battle is ongoing)
  useEffect(() => {
    if (!currentLog || battleFinished) return;

    // Parse HP from attack log: "X -> Y (...남은HP:N)"
    const attackHpMatch = currentLog.match(/(.+?)\s*->\s*(.+?)\s*\(.*남은\s*HP[:\s]*(-?\d+)/);
    if (attackHpMatch) {
      const targetPart = attackHpMatch[2].trim();
      const hp = Math.max(0, Number(attackHpMatch[3]));
      if (targetPart.includes(defenderName)) setDefenderHp(hp);
      else if (targetPart.includes(attackerName)) setAttackerHp(hp);
    }

    // Parse revival: "X의 재기! HP N로 부활!"
    const reviveMatch = currentLog.match(/(.+?)의\s*(?:재기|죽음의 보호).*HP\s*(\d+)/);
    if (reviveMatch) {
      const hp = Number(reviveMatch[2]);
      if (currentLog.includes(defenderName)) setDefenderHp(hp);
      else if (currentLog.includes(attackerName)) setAttackerHp(hp);
    }

    // Parse non-attack HP changes: bleed/poison/regen/heal
    if (!attackHpMatch && !reviveMatch) {
      const hpMatch = currentLog.match(/[（(](?:남은)?HP[:\s]*(-?\d+)[）)]/);
      if (hpMatch) {
        const hp = Math.max(0, Number(hpMatch[1]));
        const defIdx = currentLog.indexOf(defenderName);
        const atkIdx = currentLog.indexOf(attackerName);
        if (defIdx >= 0 && (atkIdx < 0 || defIdx < atkIdx)) setDefenderHp(hp);
        else if (atkIdx >= 0) setAttackerHp(hp);
      }
    }

    if (currentLog.includes('패배') || currentLog.includes('쓰러')) {
      if (currentLog.includes(attackerName)) setAttackerHp(0);
      if (currentLog.includes(defenderName)) setDefenderHp(0);
    }

    // Track potion usage
    if (currentLog.includes('사용!')) {
      const potionMatch = currentLog.match(/의\s+(.+?)\s+사용/);
      if (potionMatch) {
        setUsedPotions(prev => new Set(prev).add(potionMatch[1]));
      }
    }

    // Determine who is acting in this log line
    const attackerActing = currentLog.includes(`${attackerName}`) &&
      (currentLog.includes(`${attackerName} ->`) || currentLog.includes(`${attackerName}의`));
    const defenderActing = currentLog.includes(`${defenderName}`) &&
      (currentLog.includes(`${defenderName} ->`) || currentLog.includes(`${defenderName}의`));

    // Reset paused state for active animations
    setLeftPaused(true);
    setRightPaused(true);
    setLeftFrozenFrame(0);
    setRightFrozenFrame(0);

    const isAttackLog = currentLog.includes('명중') || currentLog.includes('크리티컬') ||
      currentLog.includes('데미지를 입') || currentLog.includes('공격');
    const isMissLog = currentLog.includes('빗나감') || currentLog.includes('차단') || currentLog.includes('회피');

    if (isAttackLog) {
      if (attackerActing) {
        setLeftAnim('attack');
        setLeftPaused(false);
        setRightAnim('hit');
        setRightPaused(false);
      } else if (defenderActing) {
        setRightAnim('attack');
        setRightPaused(false);
        setLeftAnim('hit');
        setLeftPaused(false);
      }
    } else if (isMissLog) {
      if (attackerActing) {
        setLeftAnim('attack');
        setLeftPaused(false);
      } else if (defenderActing) {
        setRightAnim('attack');
        setRightPaused(false);
      }
    } else {
      setLeftAnim('idle');
      setRightAnim('idle');
    }

    const timer = setTimeout(() => {
      if (!battleFinished) {
        setLeftAnim('idle');
        setRightAnim('idle');
        setLeftPaused(true);
        setRightPaused(true);
        setLeftFrozenFrame(0);
        setRightFrozenFrame(0);
      }
    }, 500);
    return () => clearTimeout(timer);
  }, [currentLog, logIndex]);

  const renderPotions = (potions?: PotionInfo[]) => {
    if (!potions || potions.length === 0) return null;
    return (
      <div style={{ display: 'flex', gap: 3, marginTop: 4, flexWrap: 'wrap', justifyContent: 'center' }}>
        {potions.map((p, i) => {
          const used = usedPotions.has(p.name);
          return (
            <div key={i} style={{
              fontSize: '0.55rem', padding: '2px 5px', borderRadius: 4,
              background: used ? 'rgba(100,100,100,0.3)' : 'rgba(26,188,156,0.2)',
              color: used ? '#666' : '#1abc9c',
              textDecoration: used ? 'line-through' : 'none',
            }}>
              {p.name}
            </div>
          );
        })}
      </div>
    );
  };

  const renderFighter = (
    avatar: string | null,
    charClass: string | null,
    name: string,
    hp: number,
    maxHp: number,
    anim: AnimationType,
    flip: boolean,
    paused: boolean,
    potions?: PotionInfo[],
    frozenFrame?: number,
  ) => {
    const hpPct = Math.max(0, Math.min(100, (hp / maxHp) * 100));
    const hpColor = hpPct > 50 ? '#2ecc71' : hpPct > 25 ? '#f39c12' : '#e74c3c';

    return (
      <div className="arena-fighter">
        <SpriteAvatar avatarId={avatar} animation={anim} scale={0.8} flip={flip}
          paused={paused} frozenFrame={frozenFrame} />
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
        {renderPotions(potions)}
      </div>
    );
  };

  return (
    <div className="battle-arena" style={{ background: 'var(--bg-card)', borderRadius: 12, marginBottom: 12 }}>
      {renderFighter(attackerAvatar, attackerClass, attackerName, attackerHp, attackerMaxHp,
        leftAnim, false, leftPaused, attackerPotions, leftFrozenFrame)}
      <div className="arena-vs">VS</div>
      {renderFighter(defenderAvatar, defenderClass, defenderName, defenderHp, defenderMaxHp,
        rightAnim, true, rightPaused, defenderPotions, rightFrozenFrame)}
    </div>
  );
}
