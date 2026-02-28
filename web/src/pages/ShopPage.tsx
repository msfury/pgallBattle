import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { api, type ShopResponse, type ShopPotionItem } from '../api/client';

export default function ShopPage() {
  const { id } = useParams<{ id: string }>();
  const myId = Number(id);
  const navigate = useNavigate();

  const [shop, setShop] = useState<ShopResponse | null>(null);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    api.getShop(myId).then(setShop).catch(e => {
      setError(e instanceof Error ? e.message : '상점 로딩 실패');
    });
  }, [myId]);

  const buy = async (index: number) => {
    setMessage('');
    setError('');
    setLoading(true);
    try {
      const res = await api.buyPotion(myId, index);
      setShop(res);
      setMessage('구매 완료!');
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : '구매 실패');
    } finally {
      setLoading(false);
    }
  };

  const refresh = async () => {
    setMessage('');
    setError('');
    setLoading(true);
    try {
      const res = await api.refreshShop(myId);
      setShop(res);
      setMessage('새로운 물약이 입고되었습니다!');
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : '리프레시 실패');
    } finally {
      setLoading(false);
    }
  };

  if (!shop) return <div className="text-center mt-16">{error || '로딩 중...'}</div>;

  return (
    <div>
      <button className="back-btn" onClick={() => navigate(`/mypage/${myId}`)}>← 뒤로</button>
      <h1>🏪 상점</h1>
      <p className="text-center mb-16 gold">💰 보유 골드: {shop.gold} G</p>

      <div style={{
        textAlign: 'center',
        padding: 16,
        marginBottom: 16,
        fontSize: '2rem',
      }}>
        🧙‍♂️
        <p style={{ fontSize: '0.9rem', color: '#999', marginTop: 8 }}>
          "어서오게, 모험자여. 오늘은 좋은 물약이 있다네."
        </p>
      </div>

      {shop.items.map((item: ShopPotionItem) => (
        <div className="card" key={item.index} style={{
          opacity: item.sold ? 0.5 : 1,
          marginBottom: 8,
        }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <div style={{ flex: 1 }}>
              <strong>🧪 {item.name}</strong>
              {item.sold && <span style={{ color: '#e74c3c', marginLeft: 8, fontSize: '0.8rem' }}>매진</span>}
              <p style={{ fontSize: '0.8rem', color: '#999', marginTop: 4 }}>
                {item.description}
              </p>
              <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap', marginTop: 4 }}>
                {item.effects.map((eff, i) => (
                  <span key={i} style={{
                    fontSize: '0.7rem',
                    padding: '2px 6px',
                    borderRadius: 4,
                    background: 'rgba(243, 156, 18, 0.15)',
                    color: '#f39c12',
                    border: '1px solid rgba(243, 156, 18, 0.3)',
                  }}>
                    ✨ {eff}
                  </span>
                ))}
              </div>
            </div>
            <button
              className="btn-gold"
              style={{ whiteSpace: 'nowrap', marginLeft: 12 }}
              onClick={() => buy(item.index)}
              disabled={item.sold || shop.gold < item.price || loading}
            >
              {item.sold ? '매진' : `${item.price} G`}
            </button>
          </div>
        </div>
      ))}

      {/* 리프레시 버튼 */}
      <button
        className="btn-full btn-blue"
        style={{ marginTop: 12, marginBottom: 8 }}
        onClick={refresh}
        disabled={shop.gold < shop.refreshCost || loading}
      >
        🔄 리프레시 ({shop.refreshCost} G)
        {shop.refreshCount > 0 && (
          <span style={{ fontSize: '0.8rem', opacity: 0.7 }}> - {shop.refreshCount}회 사용</span>
        )}
      </button>

      {message && <p className="text-center mt-12" style={{ color: '#2ecc71' }}>{message}</p>}
      {error && <p className="error mt-12">{error}</p>}
    </div>
  );
}
