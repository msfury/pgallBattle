export interface SpriteConfig {
  src: string;
  sheetWidth: number;
  sheetHeight: number;
  cellWidth: number;
  cellHeight: number;
  baseScale: number;
  animations: Record<string, { startCol: number; frames: number; speed: number }>;
}

export interface SpriteAvatar {
  id: string;
  name: string;
  row: number;
  classKey: string;
  sprite: SpriteConfig;
}

/** 기존 characters.png (mage/ranger/cleric 전용) */
const LEGACY_SPRITE: SpriteConfig = {
  src: '/sprites/characters.png',
  sheetWidth: 1408,
  sheetHeight: 768,
  cellWidth: 88,
  cellHeight: 96,
  baseScale: 1,
  animations: {
    idle:   { startCol: 0, frames: 3, speed: 600 },
    walk:   { startCol: 3, frames: 4, speed: 500 },
    attack: { startCol: 7, frames: 3, speed: 400 },
    hit:    { startCol: 10, frames: 3, speed: 400 },
    death:  { startCol: 13, frames: 3, speed: 600 },
  },
};

/** 하위 호환용 export */
export const SPRITE = LEGACY_SPRITE;

/** idle-only 스프라이트 (새 Pixel RPG Character Asset Pack) */
function idleOnlySprite(
  src: string, w: number, h: number,
  cellW: number, cellH: number, baseScale: number,
): SpriteConfig {
  return {
    src, sheetWidth: w, sheetHeight: h,
    cellWidth: cellW, cellHeight: cellH, baseScale,
    animations: {
      idle:   { startCol: 0, frames: 8, speed: 800 },
      walk:   { startCol: 0, frames: 8, speed: 600 },
      attack: { startCol: 0, frames: 8, speed: 400 },
      hit:    { startCol: 0, frames: 8, speed: 400 },
      death:  { startCol: 0, frames: 8, speed: 800 },
    },
  };
}

// baseScale = 96 / cellHeight → 기존 스프라이트(96px)와 동일한 표시 높이
const SWORDMAN    = idleOnlySprite('/sprites/swordman.png',    304, 308, 38, 44, 96 / 44);
const GIANT_SWORD = idleOnlySprite('/sprites/giant_sword.png', 520, 336, 65, 48, 96 / 48);
const KNIGHT      = idleOnlySprite('/sprites/knight.png',      224, 315, 28, 45, 96 / 45);
const NINJA       = idleOnlySprite('/sprites/ninja.png',       192, 315, 24, 45, 96 / 45);
const PIRATE      = idleOnlySprite('/sprites/pirate.png',      408, 273, 51, 39, 96 / 39);

export const AVATARS: SpriteAvatar[] = [
  { id: 'warrior_1', name: '검사',       row: 0, classKey: 'WARRIOR', sprite: SWORDMAN },
  { id: 'warrior_2', name: '대검 여전사', row: 0, classKey: 'WARRIOR', sprite: GIANT_SWORD },
  { id: 'warrior_3', name: '기사',       row: 0, classKey: 'WARRIOR', sprite: KNIGHT },
  { id: 'mage_1',    name: '마법사',     row: 3, classKey: 'MAGE',    sprite: LEGACY_SPRITE },
  { id: 'ranger_1',  name: '레인저',     row: 4, classKey: 'RANGER',  sprite: LEGACY_SPRITE },
  { id: 'ranger_2',  name: '사냥꾼',     row: 5, classKey: 'RANGER',  sprite: LEGACY_SPRITE },
  { id: 'rogue_1',   name: '닌자',       row: 0, classKey: 'ROGUE',   sprite: NINJA },
  { id: 'rogue_2',   name: '해적',       row: 0, classKey: 'ROGUE',   sprite: PIRATE },
  { id: 'cleric_1',  name: '성직자',     row: 7, classKey: 'CLERIC',  sprite: LEGACY_SPRITE },
];

export function getAvatarById(id: string | null): SpriteAvatar | undefined {
  if (!id) return undefined;
  return AVATARS.find(a => a.id === id);
}

export function getAvatarsForClass(classKey: string): SpriteAvatar[] {
  return AVATARS.filter(a => a.classKey === classKey);
}
