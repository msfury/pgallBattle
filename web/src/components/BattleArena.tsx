import { useEffect, useState } from 'react';
import { CLASS_COLOR } from '../data/classes';
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
}

export default function BattleArena({
  attackerAvatar, defenderAvatar, attackerClass, defenderClass,
  attackerName, defenderName, attackerMaxHp, defenderMaxHp,
  currentLog, logIndex, battleFinished, winnerId, attackerId, defenderId,
}: BattleArenaProps) {
  const [attackerHp, setAttackerHp] = useState(attackerMaxHp);
  const [defenderHp, setDefenderHp] = useState(defenderMaxHp);
  const [leftAnim, setLeftAnim] = useState<AnimationType>('idle');
  const [rightAnim, setRightAnim] = useState<AnimationType>('idle');
  const [leftPaused, setLeftPaused] = useState(false);
  const [rightPaused, setRightPaused] = useState(false);
  const [leftFrozenFrame, setLeftFrozenFrame] = useState<number | undefined>(undefined);
  const [rightFrozenFrame, setRightFrozenFrame] = useState<number | undefined>(undefined);

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
      setRightFrozenFrame(2); // last frame of death
    } else {
      setRightAnim('idle');
      setRightPaused(true);
      setRightFrozenFrame(0);
      setLeftAnim('death');
      setLeftPaused(true);
      setLeftFrozenFrame(2); // last frame of death
    }
  }, [battleFinished, winnerId, attackerId, defenderId]);

  // Handle log-based animations (only when battle is ongoing)
  useEffect(() => {
    if (!currentLog || battleFinished) return;

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
      // Non-combat log (buffs, potions, round markers): both paused at idle
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

  const renderFighter = (
    avatar: string | null,
    charClass: string | null,
    name: string,
    hp: number,
    maxHp: number,
    anim: AnimationType,
    flip: boolean,
    paused: boolean,
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
      </div>
    );
  };

  return (
    <div className="battle-arena" style={{ background: 'var(--bg-card)', borderRadius: 12, marginBottom: 12 }}>
      {renderFighter(attackerAvatar, attackerClass, attackerName, attackerHp, attackerMaxHp,
        leftAnim, false, leftPaused, leftFrozenFrame)}
      <div className="arena-vs">VS</div>
      {renderFighter(defenderAvatar, defenderClass, defenderName, defenderHp, defenderMaxHp,
        rightAnim, true, rightPaused, rightFrozenFrame)}
    </div>
  );
}
