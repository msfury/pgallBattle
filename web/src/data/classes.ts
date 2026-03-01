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
  { key: 'ROGUE',   name: '도적', emoji: '🗡️', desc: 'DEX 기반 | 급소공격 +1d6 | 선호무기 보정', color: '#9b59b6', primaryStat: 'DEX' },
  { key: 'MAGE',    name: '마법사', emoji: '🔮', desc: 'INT 기반 | 마법무기 +3 | 선호무기 보정', color: '#3498db', primaryStat: 'INT' },
  { key: 'CLERIC',  name: '성직자', emoji: '✝️', desc: 'WIS 기반 | HP회복 | 신성공격 | 선호무기 보정', color: '#2ecc71', primaryStat: 'WIS' },
  { key: 'RANGER',  name: '궁수', emoji: '🏹', desc: 'DEX 기반 | 활 +2 | 속사 | 선호무기 보정', color: '#f39c12', primaryStat: 'DEX' },
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

export const CLASS_TOOLTIP: Record<string, string> = {
  WARRIOR: '근접 물리무기 데미지 +2\nHP가 가장 높음 (hitDie 12)',
  ROGUE: '첫 라운드 급소공격 +1d6\n선호무기: 단검, 클로, 레이피어 (50% 보정)',
  MAGE: '마법무기(지팡이/완드) 데미지 +3\n선호무기: 지팡이, 완드 (50% 보정)',
  CLERIC: '매 라운드 HP 회복 (1+WIS보정)\n신성무기(철퇴/플레일/완드) 데미지 +1+WIS보정\n선호무기: 철퇴, 플레일, 완드 (50% 보정)',
  RANGER: '활 데미지 +2, 50% 확률 속사(2회 공격)\n선호무기: 활 (50% 보정)',
};
