export interface ClassInfo {
  key: string;
  name: string;
  emoji: string;
  desc: string;
  color: string;
  primaryStat: string;
}

export const CLASSES: ClassInfo[] = [
  { key: 'WARRIOR', name: '전사', emoji: '⚔️', desc: 'STR 기반 | HP 최고 | 근접 데미지 +2', color: '#e74c3c', primaryStat: 'STR' },
  { key: 'ROGUE',   name: '도적', emoji: '🗡️', desc: 'DEX 기반 | 급소공격 +1d6 | AC +1', color: '#9b59b6', primaryStat: 'DEX' },
  { key: 'MAGE',    name: '마법사', emoji: '🔮', desc: 'INT 기반 | 마법무기 데미지 +3', color: '#3498db', primaryStat: 'INT' },
  { key: 'CLERIC',  name: '성직자', emoji: '✝️', desc: 'WIS 기반 | 매 라운드 HP 회복 | AC +1', color: '#2ecc71', primaryStat: 'WIS' },
  { key: 'RANGER',  name: '궁수', emoji: '🏹', desc: 'DEX 기반 | 활 데미지 +2 | 속사', color: '#f39c12', primaryStat: 'DEX' },
];

export const CLASS_EMOJI: Record<string, string> = {
  WARRIOR: '⚔️',
  ROGUE: '🗡️',
  MAGE: '🔮',
  CLERIC: '✝️',
  RANGER: '🏹',
};

export const CLASS_COLOR: Record<string, string> = {
  WARRIOR: '#e74c3c',
  ROGUE: '#9b59b6',
  MAGE: '#3498db',
  CLERIC: '#2ecc71',
  RANGER: '#f39c12',
};
