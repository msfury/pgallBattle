import { BrowserRouter, Routes, Route } from 'react-router-dom';
import HomePage from './pages/HomePage';
import CharacterCreatePage from './pages/CharacterCreatePage';
import MyPage from './pages/MyPage';
import ShopPage from './pages/ShopPage';
import GachaPage from './pages/GachaPage';
import BattlePage from './pages/BattlePage';

function App() {
  return (
    <BrowserRouter>
      <div className="app">
        <Routes>
          <Route path="/" element={<HomePage />} />
          <Route path="/create" element={<CharacterCreatePage />} />
          <Route path="/mypage/:id" element={<MyPage />} />
          <Route path="/shop/:id" element={<ShopPage />} />
          <Route path="/gacha/:id" element={<GachaPage />} />
          <Route path="/battle/:attackerId/:defenderId" element={<BattlePage />} />
        </Routes>
      </div>
    </BrowserRouter>
  );
}

export default App;
