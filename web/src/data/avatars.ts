export interface SpriteAvatar {
  id: string;
  name: string;
  row: number;
  classKey: string;
}

export const SPRITE = {
  src: '/sprites/characters.png',
  sheetWidth: 1408,
  sheetHeight: 768,
  cellWidth: 88,
  cellHeight: 96,
  cols: 16,
  rows: 8,
  animations: {
    idle:   { startCol: 0, frames: 3, speed: 600 },
    walk:   { startCol: 3, frames: 4, speed: 500 },
    attack: { startCol: 7, frames: 3, speed: 400 },
    hit:    { startCol: 10, frames: 3, speed: 400 },
    death:  { startCol: 13, frames: 3, speed: 600 },
  } as Record<string, { startCol: number; frames: number; speed: number }>,
};

export const AVATARS: SpriteAvatar[] = [
  { id: 'warrior_1', name: '중갑 전사',  row: 0, classKey: 'WARRIOR' },
  { id: 'warrior_2', name: '기사',       row: 1, classKey: 'WARRIOR' },
  { id: 'warrior_3', name: '용병',       row: 2, classKey: 'WARRIOR' },
  { id: 'mage_1',    name: '마법사',     row: 3, classKey: 'MAGE' },
  { id: 'ranger_1',  name: '레인저',     row: 4, classKey: 'RANGER' },
  { id: 'ranger_2',  name: '사냥꾼',     row: 5, classKey: 'RANGER' },
  { id: 'rogue_1',   name: '암살자',     row: 6, classKey: 'ROGUE' },
  { id: 'cleric_1',  name: '성직자',     row: 7, classKey: 'CLERIC' },
];

export function getAvatarById(id: string | null): SpriteAvatar | undefined {
  if (!id) return undefined;
  return AVATARS.find(a => a.id === id);
}

export function getAvatarsForClass(classKey: string): SpriteAvatar[] {
  return AVATARS.filter(a => a.classKey === classKey);
}
