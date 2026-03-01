import { useEffect, useRef, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { api, type BattleResult } from '../api/client';
import BattleArena from '../components/BattleArena';

export default function BattlePage() {
  const { attackerId, defenderId } = useParams();
  const navigate = useNavigate();

  const [result, setResult] = useState<BattleResult | null>(null);
  const [displayedLogs, setDisplayedLogs] = useState<string[]>([]);
  const [currentLogIndex, setCurrentLogIndex] = useState(-1);
  const [battling, setBattling] = useState(true);
  const [battleFinished, setBattleFinished] = useState(false);
  const [error, setError] = useState('');
  const logEndRef = useRef<HTMLDivElement>(null);
  const battleStarted = useRef(false);

  useEffect(() => {
    if (battleStarted.current) return;
    battleStarted.current = true;
    startBattle();
  }, []);

  useEffect(() => {
    logEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [displayedLogs]);

  const getLogDelay = (log: string): number => {
    // Attack/damage logs get longer delay for animation sync
    if (log.includes('명중') || log.includes('크리티컬') || log.includes('데미지를 입') ||
        log.includes('빗나감') || log.includes('차단')) {
      return 800;
    }
    // Round markers
    if (log.includes('===')) return 600;
    // Victory/defeat
    if (log.includes('승리') || log.includes('패배')) return 1000;
    // Everything else (buffs, potions, status effects)
    return 400;
  };

  const startBattle = async () => {
    try {
      const res = await api.battle(Number(attackerId), Number(defenderId));
      setResult(res);
      for (let i = 0; i < res.battleLog.length; i++) {
        await delay(getLogDelay(res.battleLog[i]));
        setDisplayedLogs((prev) => [...prev, res.battleLog[i]]);
        setCurrentLogIndex(i);
      }
      setBattleFinished(true);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : '전투 실패');
    } finally {
      setBattling(false);
    }
  };

  const delay = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms));

  const currentLog = result && currentLogIndex >= 0 ? result.battleLog[currentLogIndex] : null;

  return (
    <div>
      <h1>⚔️ 전투</h1>

      {/* Battle Arena */}
      {result && (
        <BattleArena
          attackerAvatar={result.attackerAvatar}
          defenderAvatar={result.defenderAvatar}
          attackerClass={result.attackerClass}
          defenderClass={result.defenderClass}
          attackerName={result.attackerName}
          defenderName={result.defenderName}
          attackerMaxHp={result.attackerMaxHp}
          defenderMaxHp={result.defenderMaxHp}
          attackerFinalHp={result.attackerFinalHp}
          defenderFinalHp={result.defenderFinalHp}
          currentLog={currentLog}
          logIndex={currentLogIndex}
          battleFinished={battleFinished}
          winnerId={result.winnerId}
          attackerId={Number(attackerId)}
          defenderId={Number(defenderId)}
          attackerPotions={result.attackerPotions}
          defenderPotions={result.defenderPotions}
        />
      )}

      {/* Battle Log */}
      <div className="card" style={{
        maxHeight: '40vh', overflow: 'auto', marginBottom: 16,
        fontFamily: 'monospace', fontSize: '0.85rem', lineHeight: 1.8,
      }}>
        {displayedLogs.map((log, i) => (
          <div key={i} style={{
            color: log.includes('크리티컬') ? '#e74c3c' :
                   log.includes('승리') ? '#ffd700' :
                   log.includes('빗나감') || log.includes('회피') ? '#666' :
                   log.includes('===') ? '#6c5ce7' :
                   log.includes('---') ? '#555' :
                   log.includes('발동') || log.includes('효과') ? '#f39c12' :
                   log.includes('기절') || log.includes('침묵') || log.includes('무장 해제') ? '#9b59b6' :
                   log.includes('차단') || log.includes('보호막') ? '#3498db' :
                   log.includes('독') || log.includes('출혈') ? '#27ae60' :
                   log.includes('흡수') || log.includes('흡혈') ? '#e74c3c' :
                   log.includes('회복') || log.includes('재생') ? '#2ecc71' :
                   log.includes('화염') || log.includes('빙결') || log.includes('번개') ? '#e67e22' :
                   log.includes('물약') ? '#1abc9c' :
                   '#ccc',
            fontWeight: log.includes('===') || log.includes('승리') ? 'bold' : 'normal',
            padding: '1px 0',
          }}>
            {log}
          </div>
        ))}
        {battling && <div style={{ color: '#999' }}>전투 진행 중...</div>}
        <div ref={logEndRef} />
      </div>

      {/* Result */}
      {result && !battling && (
        <div className="card text-center mb-12" style={{
          background: 'linear-gradient(135deg, rgba(255,215,0,0.1), rgba(108,92,231,0.1))',
          border: '1px solid rgba(255,215,0,0.3)',
        }}>
          <div style={{ fontSize: '2rem', marginBottom: 8 }}>🏆</div>
          <div style={{ fontSize: '1.2rem', fontWeight: 'bold', color: '#ffd700' }}>
            {result.winnerName} 승리!
          </div>
          <div style={{ color: '#999', marginTop: 4 }}>
            +{result.goldReward} 골드 획득
          </div>
        </div>
      )}

      {error && <p className="error mb-12">{error}</p>}

      {!battling && (
        <button className="btn-full btn-blue" onClick={() => navigate('/')}>
          돌아가기
        </button>
      )}
    </div>
  );
}
