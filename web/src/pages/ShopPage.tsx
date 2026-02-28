import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { api, type Character, type ShopItem } from '../api/client';

export default function ShopPage() {
  const { id } = useParams<{ id: string }>();
  const myId = Number(id);
  const navigate = useNavigate();

  const [me, setMe] = useState<Character | null>(null);
  const [items, setItems] = useState<ShopItem[]>([]);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');

  useEffect(() => {
    Promise.all([api.getCharacter(myId), api.getShopItems()]).then(([c, i]) => {
      setMe(c);
      setItems(i);
    });
  }, [myId]);

  const buy = async (itemId: number) => {
    setMessage('');
    setError('');
    try {
      await api.buyItem(myId, itemId);
      const updated = await api.getCharacter(myId);
      setMe(updated);
      setMessage('구매 완료!');
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : '구매 실패');
    }
  };

  if (!me) return <div className="text-center mt-16">로딩 중...</div>;

  return (
    <div>
      <button className="back-btn" onClick={() => navigate(`/mypage/${myId}`)}>← 뒤로</button>
      <h1>🏪 상점</h1>
      <p className="text-center mb-16 gold">💰 보유 골드: {me.gold} G</p>

      <div style={{
        textAlign: 'center',
        padding: 16,
        marginBottom: 16,
        fontSize: '2rem',
      }}>
        🧙‍♂️
        <p style={{ fontSize: '0.9rem', color: '#999', marginTop: 8 }}>
          "어서오게, 모험자여. 좋은 물건이 있다네."
        </p>
      </div>

      {items.map((item) => (
        <div className="card" key={item.id}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <div>
              <strong>{item.name}</strong>
              <p style={{ fontSize: '0.8rem', color: '#999', marginTop: 4 }}>
                {item.description}
              </p>
            </div>
            <button
              className="btn-gold"
              style={{ whiteSpace: 'nowrap', marginLeft: 12 }}
              onClick={() => buy(item.id)}
              disabled={me.gold < item.price}
            >
              {item.price} G
            </button>
          </div>
        </div>
      ))}

      {message && <p className="text-center mt-12" style={{ color: '#2ecc71' }}>{message}</p>}
      {error && <p className="error mt-12">{error}</p>}
    </div>
  );
}
