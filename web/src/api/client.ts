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

export interface ShopItem {
  id: number;
  name: string;
  description: string;
  price: number;
  buffType: string;
  buffChance: number;
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
}

export const api = {
  randomStats: () => request<Stats>('/characters/random-stats'),
  createCharacter: (data: Stats & { name: string; avatar?: string; characterClass?: string }) =>
    request<Character>('/characters', { method: 'POST', body: JSON.stringify(data) }),
  getCharacters: () => request<Character[]>('/characters'),
  getRanking: () => request<Character[]>('/characters/ranking'),
  getCharacter: (id: number) => request<Character>(`/characters/${id}`),
  gacha: (characterId: number) =>
    request<Equipment>(`/gacha/${characterId}`, { method: 'POST' }),
  getShopItems: () => request<ShopItem[]>('/shop/items'),
  buyItem: (characterId: number, shopItemId: number) =>
    request<unknown>('/shop/buy', {
      method: 'POST',
      body: JSON.stringify({ characterId, shopItemId }),
    }),
  battle: (attackerId: number, defenderId: number) =>
    request<BattleResult>('/battle', {
      method: 'POST',
      body: JSON.stringify({ attackerId, defenderId }),
    }),
  equipItem: (characterId: number, equipmentId: number) =>
    request<Equipment>(`/characters/${characterId}/equipment/${equipmentId}/equip`, { method: 'PUT' }),
  unequipItem: (characterId: number, equipmentId: number) =>
    request<Equipment>(`/characters/${characterId}/equipment/${equipmentId}/unequip`, { method: 'PUT' }),
};
