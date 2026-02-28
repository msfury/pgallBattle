import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api, type Stats } from '../api/client';
import { CLASSES } from '../data/classes';
import { getAvatarsForClass } from '../data/avatars';
import SpriteAvatar from '../components/SpriteAvatar';

const STAT_NAMES: Record<keyof Stats, string> = {
  strength: 'STR 힘', dexterity: 'DEX 민첩', constitution: 'CON 체력',
  intelligence: 'INT 지능', wisdom: 'WIS 지혜', charisma: 'CHA 매력',
};

export default function CharacterCreatePage() {
  const navigate = useNavigate();
  const [name, setName] = useState('');
  const [stats, setStats] = useState<Stats | null>(null);
  const [rolling, setRolling] = useState(false);
  const [creating, setCreating] = useState(false);
  const [error, setError] = useState('');
  const [selectedAvatar, setSelectedAvatar] = useState('');
  const [selectedClass, setSelectedClass] = useState('');

  const availableAvatars = selectedClass ? getAvatarsForClass(selectedClass) : [];

  const rollStats = async () => {
    setRolling(true);
    setError('');
    try {
      setStats(await api.randomStats());
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : '스탯 생성 실패');
    } finally {
      setRolling(false);
    }
  };

  const createCharacter = async () => {
    if (!stats || !name.trim() || !selectedClass || !selectedAvatar) return;
    setCreating(true);
    setError('');
    try {
      const character = await api.createCharacter({
        name: name.trim(), avatar: selectedAvatar, characterClass: selectedClass, ...stats,
      });
      localStorage.setItem('myCharId', String(character.id));
      navigate(`/mypage/${character.id}`);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : '캐릭터 생성 실패');
    } finally {
      setCreating(false);
    }
  };

  const total = stats ? Object.values(stats).reduce((a, b) => a + b, 0) : 0;

  return (
    <div>
      <button className="back-btn" onClick={() => navigate('/')}>← 홈</button>
      <h1>캐릭터 생성</h1>

      <div className="card mb-12">
        <input type="text" placeholder="캐릭터 이름을 입력하세요" value={name}
          onChange={e => setName(e.target.value)} maxLength={20} />
      </div>

      {/* 직업 선택 */}
      <div className="card mb-12">
        <h2>직업 선택</h2>
        <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap' }}>
          {CLASSES.map(c => (
            <div key={c.key} onClick={() => { setSelectedClass(c.key); setSelectedAvatar(''); }} style={{
              flex: '1 1 calc(50% - 6px)', padding: '10px 8px', borderRadius: 8,
              border: selectedClass === c.key ? `2px solid ${c.color}` : '2px solid rgba(255,255,255,0.08)',
              background: selectedClass === c.key ? `${c.color}22` : 'rgba(255,255,255,0.03)',
              cursor: 'pointer', textAlign: 'center', transition: 'all 0.2s',
            }}>
              <div style={{ fontSize: '1.3rem' }}>{c.emoji}</div>
              <div style={{ fontWeight: 'bold', fontSize: '0.9rem', color: c.color }}>{c.name}</div>
              <div style={{ fontSize: '0.65rem', color: '#999', marginTop: 2 }}>{c.desc}</div>
            </div>
          ))}
        </div>
      </div>

      {/* 아바타 선택 (직업별 스프라이트) */}
      {selectedClass && (
        <div className="card mb-12">
          <h2>아바타 선택</h2>
          <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', justifyContent: 'center' }}>
            {availableAvatars.map(a => (
              <div key={a.id} onClick={() => setSelectedAvatar(a.id)} style={{
                padding: 6, borderRadius: 8, cursor: 'pointer', textAlign: 'center',
                border: selectedAvatar === a.id ? '2px solid var(--primary)' : '2px solid transparent',
                background: selectedAvatar === a.id ? 'rgba(108,92,231,0.2)' : 'rgba(255,255,255,0.03)',
              }}>
                <SpriteAvatar avatarId={a.id} animation="idle" scale={0.7} />
                <div style={{ fontSize: '0.75rem', color: '#ccc', marginTop: 2 }}>{a.name}</div>
              </div>
            ))}
          </div>
        </div>
      )}

      <button className="btn-full btn-blue mb-12" onClick={rollStats} disabled={rolling}>
        {rolling ? '주사위 굴리는 중...' : '🎲 능력치 굴리기 (4d6 drop lowest)'}
      </button>

      {stats && (
        <div className="card mb-12">
          <h2>능력치 <span style={{ fontSize: '0.85rem', color: '#999' }}>합계: {total}</span></h2>
          <div className="stat-grid">
            {(Object.keys(STAT_NAMES) as (keyof Stats)[]).map(key => (
              <div className="stat-item" key={key}>
                <span className="stat-label">{STAT_NAMES[key]}</span>
                <span className="stat-value">{stats[key]}</span>
              </div>
            ))}
          </div>
        </div>
      )}

      {error && <p className="error mb-12">{error}</p>}

      <button className="btn-full btn-green" onClick={createCharacter}
        disabled={!stats || !name.trim() || !selectedClass || !selectedAvatar || creating}>
        {creating ? '생성 중...' : '이 능력치로 캐릭터 생성'}
      </button>
    </div>
  );
}
