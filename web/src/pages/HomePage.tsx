import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api, type Character } from '../api/client';
import { CLASS_EMOJI, CLASS_COLOR } from '../data/classes';
import SpriteAvatar from '../components/SpriteAvatar';

export default function HomePage() {
  const navigate = useNavigate();
  const [characters, setCharacters] = useState<Character[]>([]);
  const [error, setError] = useState('');
  const [myId, setMyId] = useState(Number(localStorage.getItem('myCharId') || 0));

  useEffect(() => {
    api.getRanking()
      .then(setCharacters)
      .catch(e => setError(e instanceof Error ? e.message : '로딩 실패'));

    // 저장된 캐릭터가 실제로 존재하는지 확인
    if (myId > 0) {
      api.getCharacter(myId).catch(() => {
        localStorage.removeItem('myCharId');
        setMyId(0);
      });
    }
  }, []);

  return (
    <div>
      <h1>⚔️ pgallBattle</h1>

      <div className="flex-row mb-16">
        {myId > 0 ? (
          <button className="btn-blue" onClick={() => navigate(`/mypage/${myId}`)}>👤 내 캐릭터</button>
        ) : (
          <button className="btn-green" onClick={() => navigate('/create')}>✨ 캐릭터 생성</button>
        )}
      </div>

      <h2>🏆 ELO 랭킹</h2>
      {characters.length === 0 && !error && (
        <p style={{ color: '#999', textAlign: 'center', padding: 16 }}>아직 모험자가 없습니다</p>
      )}
      {characters.map((c, i) => (
        <div className="card" key={c.id}
          style={{ cursor: myId > 0 && myId !== c.id ? 'pointer' : 'default', display: 'flex', alignItems: 'center', gap: 10 }}
          onClick={() => {
            if (myId > 0 && myId !== c.id) navigate(`/battle/${myId}/${c.id}`);
          }}>
          <div style={{
            width: 28, textAlign: 'center', fontWeight: 'bold', fontSize: '1rem',
            color: i === 0 ? '#ffd700' : i === 1 ? '#c0c0c0' : i === 2 ? '#cd7f32' : '#999',
          }}>
            {i + 1}
          </div>
          <SpriteAvatar avatarId={c.avatar} animation="idle" scale={0.55} />
          <div style={{ flex: 1 }}>
            <div>
              <strong>{c.name}</strong>
              {c.id === myId && <span style={{ fontSize: '0.7rem', color: 'var(--primary)', marginLeft: 6 }}>(나)</span>}
            </div>
            <div style={{ fontSize: '0.8rem', color: '#999' }}>
              Lv.{c.level}
              {c.characterClass && (
                <span style={{ color: CLASS_COLOR[c.characterClass] || '#999', marginLeft: 4 }}>
                  {CLASS_EMOJI[c.characterClass]} {c.classKoreanName}
                </span>
              )}
            </div>
          </div>
          <div style={{ textAlign: 'right' }}>
            <div style={{ fontWeight: 'bold', color: '#ffd700', fontSize: '1.1rem' }}>{c.eloRate}</div>
            <div style={{ fontSize: '0.7rem', color: '#999' }}>ELO</div>
          </div>
          {myId > 0 && myId !== c.id && (
            <div style={{ fontSize: '0.8rem', color: 'var(--red)' }}>⚔️</div>
          )}
        </div>
      ))}

      {error && <p className="error mt-12">{error}</p>}
    </div>
  );
}
