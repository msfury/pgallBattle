const BASE = '/api';

async function request<T>(path: string, options?: RequestInit): Promise<T> {
  const res = await fetch(`${BASE}${path}`, {
    headers: { 'Content-Type': 'application/json' },
    ...options,
  });
  if (!res.ok) {
    const err = await res.json().catch(() => ({ error: res.statusText }));
    throw new Error(err.error || res.statusText);
  }
  return res.json();
}

export interface Stats {
  strength: number;
  dexterity: number;
  constitution: number;
  intelligence: number;
  wisdom: number;
  charisma: number;
}

export interface Equipment {
  id: number;
  name: string;
  type: string;
  grade: string;
  attackBonus: number;
  defenseBonus: number;
  effect: string | null;
  effectChance: number;
  effectValue: number;
  equipped: boolean;
  weaponCategory: string | null;
  scalingStat: string | null;
  twoHanded: boolean;
  baseDamageMin: number;
  baseDamageMax: number;
}

export interface Character {
  id: number;
  name: string;
  avatar: string | null;
  characterClass: string | null;
  classKoreanName: string | null;
  strength: number;
  dexterity: number;
  constitution: number;
  intelligence: number;
  wisdom: number;
  charisma: number;
  level: number;
  hp: number;
  maxHp: number;
  gold: number;
  eloRate: number;
  equipments: Equipment[];
}

export interface ShopPotionItem {
  index: number;
  name: string;
  description: string;
  price: number;
  effects: string[];
  sold: boolean;
}

export interface ShopResponse {
  items: ShopPotionItem[];
  refreshCost: number;
  refreshCount: number;
  gold: number;
}

export interface PotionInfo {
  name: string;
  buffType: string;
  quantity: number;
}

export interface BattleResult {
  winnerId: number;
  winnerName: string;
  loserId: number;
  loserName: string;
  battleLog: string[];
  goldReward: number;
  attackerName: string;
  defenderName: string;
  attackerAvatar: string | null;
  defenderAvatar: string | null;
  attackerClass: string | null;
  defenderClass: string | null;
  attackerMaxHp: number;
  defenderMaxHp: number;
  attackerPotions: PotionInfo[];
  defenderPotions: PotionInfo[];
}

export const api = {
  randomStats: () => request<Stats>('/characters/random-stats'),
  createCharacter: (data: Stats & { name: string; avatar?: string; characterClass?: string }) =>
    request<Character>('/characters', { method: 'POST', body: JSON.stringify(data) }),
  getCharacters: () => request<Character[]>('/characters'),
  getRanking: () => request<Character[]>('/characters/ranking'),
  getCharacter: (id: number) => request<Character>(`/characters/${id}`),
  getMyCharacter: () => request<Character>('/characters/mine'),
  dailyCheck: (characterId: number) =>
    request<{ granted: boolean; amount: number }>(`/characters/${characterId}/daily-check`, { method: 'POST' }),
  gacha: (characterId: number) =>
    request<Equipment>(`/gacha/${characterId}`, { method: 'POST' }),
  getShop: (characterId: number) =>
    request<ShopResponse>(`/shop/${characterId}/items`),
  refreshShop: (characterId: number) =>
    request<ShopResponse>(`/shop/${characterId}/refresh`, { method: 'POST' }),
  buyPotion: (characterId: number, index: number) =>
    request<ShopResponse>(`/shop/${characterId}/buy/${index}`, { method: 'POST' }),
  battle: (attackerId: number, defenderId: number) =>
    request<BattleResult>('/battle', {
      method: 'POST',
      body: JSON.stringify({ attackerId, defenderId }),
    }),
  deleteCharacter: (id: number) =>
    request<void>(`/characters/${id}`, { method: 'DELETE' }),
  sellEquipment: (characterId: number, equipmentId: number) =>
    request<{ soldPrice: number }>(`/characters/${characterId}/equipment/${equipmentId}/sell`, { method: 'POST' }),
  equipItem: (characterId: number, equipmentId: number) =>
    request<Equipment>(`/characters/${characterId}/equipment/${equipmentId}/equip`, { method: 'PUT' }),
  unequipItem: (characterId: number, equipmentId: number) =>
    request<Equipment>(`/characters/${characterId}/equipment/${equipmentId}/unequip`, { method: 'PUT' }),
};
