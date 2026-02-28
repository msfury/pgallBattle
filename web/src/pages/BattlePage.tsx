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
  const [error, setError] = useState('');
  const logEndRef = useRef<HTMLDivElement>(null);

  useEffect(() => { startBattle(); }, []);

  useEffect(() => {
    logEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [displayedLogs]);

  const startBattle = async () => {
    try {
      const res = await api.battle(Number(attackerId), Number(defenderId));
      setResult(res);
      for (let i = 0; i < res.battleLog.length; i++) {
        await delay(400);
        setDisplayedLogs((prev) => [...prev, res.battleLog[i]]);
        setCurrentLogIndex(i);
      }
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
          currentLog={currentLog}
          logIndex={currentLogIndex}
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
                   log.includes('발동') ? '#f39c12' :
                   log.includes('기절') ? '#9b59b6' :
                   log.includes('차단') ? '#3498db' :
                   log.includes('독') ? '#2ecc71' :
                   log.includes('흡수') || log.includes('흡혈') ? '#e74c3c' :
                   log.includes('회복') ? '#2ecc71' :
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
