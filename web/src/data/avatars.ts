export interface SpriteConfig {
  src: string;
  sheetWidth: number;
  sheetHeight: number;
  cellWidth: number;
  cellHeight: number;
  baseScale: number;
  animations: Record<string, { startCol: number; frames: number; speed: number }>;
}

/** 스트립(파일별 애니메이션) 스프라이트 설정 */
export interface StripSpriteConfig {
  type: 'strip';
  cellWidth: number;
  cellHeight: number;
  baseScale: number;
  animations: Record<string, {
    strips: { src: string; frames: number }[];
    speed: number;
    randomPick?: boolean; // true면 매 재생마다 strips 중 하나를 랜덤 선택
  }>;
}

export type AnySpriteConfig = SpriteConfig | StripSpriteConfig;

export function isStripSprite(sprite: AnySpriteConfig): sprite is StripSpriteConfig {
  return 'type' in sprite && sprite.type === 'strip';
}

export interface SpriteAvatar {
  id: string;
  name: string;
  row: number;
  classKey: string;
  sprite: AnySpriteConfig;
}

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

// ── 128×128 스트립 스프라이트 ───────────────────────────────
const S = 96 / 128; // baseScale: 128px → 96px 표시 높이

const WORRIOR_SPRITE: StripSpriteConfig = {
  type: 'strip', cellWidth: 128, cellHeight: 128, baseScale: S,
  animations: {
    idle:   { strips: [{ src: '/sprites/WORRIOR/Idle.png', frames: 8 }], speed: 800 },
    walk:   { strips: [{ src: '/sprites/WORRIOR/Walk.png', frames: 8 }], speed: 600 },
    attack: {
      strips: [
        { src: '/sprites/WORRIOR/Attack_1.png', frames: 6 },
        { src: '/sprites/WORRIOR/Attack_2.png', frames: 3 },
        { src: '/sprites/WORRIOR/Attack_3.png', frames: 4 },
      ],
      speed: 500, randomPick: true,
    },
    hit:    { strips: [{ src: '/sprites/WORRIOR/Hurt.png', frames: 3 }], speed: 400 },
    death:  { strips: [{ src: '/sprites/WORRIOR/Dead.png', frames: 3 }], speed: 600 },
  },
};

const WOMAN_KNIGHT_SPRITE: StripSpriteConfig = {
  type: 'strip', cellWidth: 128, cellHeight: 128, baseScale: S,
  animations: {
    idle:   { strips: [{ src: '/sprites/WOMAN_KNIGHT/Idle.png', frames: 6 }], speed: 800 },
    walk:   { strips: [{ src: '/sprites/WOMAN_KNIGHT/Walk.png', frames: 8 }], speed: 600 },
    attack: {
      strips: [
        { src: '/sprites/WOMAN_KNIGHT/Attack_1.png', frames: 5 },
        { src: '/sprites/WOMAN_KNIGHT/Attack_2.png', frames: 2 },
        { src: '/sprites/WOMAN_KNIGHT/Attack_3.png', frames: 5 },
        { src: '/sprites/WOMAN_KNIGHT/Attack_4.png', frames: 5 },
      ],
      speed: 500, randomPick: true,
    },
    hit:    { strips: [{ src: '/sprites/WOMAN_KNIGHT/Hurt.png', frames: 3 }], speed: 400 },
    death:  { strips: [{ src: '/sprites/WOMAN_KNIGHT/Dead.png', frames: 4 }], speed: 600 },
  },
};

const MAGE_SPRITE: StripSpriteConfig = {
  type: 'strip', cellWidth: 128, cellHeight: 128, baseScale: S,
  animations: {
    idle:   { strips: [{ src: '/sprites/MAGE/Idle.png', frames: 6 }], speed: 800 },
    walk:   { strips: [{ src: '/sprites/MAGE/Walk.png', frames: 7 }], speed: 600 },
    attack: {
      strips: [
        { src: '/sprites/MAGE/Attack_1.png', frames: 10 },
        { src: '/sprites/MAGE/Attack_2.png', frames: 4 },
        { src: '/sprites/MAGE/Attack_3.png', frames: 7 },
      ],
      speed: 500, randomPick: true,
    },
    hit:    { strips: [{ src: '/sprites/MAGE/Hurt.png', frames: 4 }], speed: 400 },
    death:  { strips: [{ src: '/sprites/MAGE/Dead.png', frames: 4 }], speed: 600 },
  },
};

const RANGER_SPRITE: StripSpriteConfig = {
  type: 'strip', cellWidth: 128, cellHeight: 128, baseScale: S,
  animations: {
    idle:   { strips: [{ src: '/sprites/RANGER/Idle.png', frames: 6 }], speed: 800 },
    walk:   { strips: [{ src: '/sprites/RANGER/Walk.png', frames: 8 }], speed: 600 },
    attack: {
      strips: [
        { src: '/sprites/RANGER/Attack_1.png', frames: 4 },
        { src: '/sprites/RANGER/Shot_1.png', frames: 14 },
        { src: '/sprites/RANGER/Shot_2.png', frames: 13 },
      ],
      speed: 500, randomPick: true,
    },
    hit:    { strips: [{ src: '/sprites/RANGER/Hurt.png', frames: 3 }], speed: 400 },
    death:  { strips: [{ src: '/sprites/RANGER/Dead.png', frames: 3 }], speed: 600 },
  },
};

const ROGUE_SPRITE: StripSpriteConfig = {
  type: 'strip', cellWidth: 128, cellHeight: 128, baseScale: S,
  animations: {
    idle:   { strips: [{ src: '/sprites/ROGUE/Idle.png', frames: 5 }], speed: 800 },
    walk:   { strips: [{ src: '/sprites/ROGUE/Walk.png', frames: 8 }], speed: 600 },
    attack: {
      strips: [
        { src: '/sprites/ROGUE/Attack_1.png', frames: 5 },
        { src: '/sprites/ROGUE/Attack_2.png', frames: 4 },
        { src: '/sprites/ROGUE/Attack_3.png', frames: 6 },
        { src: '/sprites/ROGUE/Attack_4.png', frames: 5 },
      ],
      speed: 500, randomPick: true,
    },
    hit:    { strips: [{ src: '/sprites/ROGUE/Hurt.png', frames: 2 }], speed: 400 },
    death:  { strips: [{ src: '/sprites/ROGUE/Dead.png', frames: 4 }], speed: 600 },
  },
};

const CLERIC_SPRITE: StripSpriteConfig = {
  type: 'strip', cellWidth: 128, cellHeight: 128, baseScale: S,
  animations: {
    idle:   { strips: [{ src: '/sprites/CLERIC/Idle.png', frames: 5 }], speed: 800 },
    walk:   { strips: [{ src: '/sprites/CLERIC/Walk.png', frames: 8 }], speed: 600 },
    attack: {
      strips: [
        { src: '/sprites/CLERIC/Attack_1.png', frames: 6 },
        { src: '/sprites/CLERIC/Attack_2.png', frames: 3 },
        { src: '/sprites/CLERIC/Attack_3.png', frames: 3 },
        { src: '/sprites/CLERIC/Attack_4.png', frames: 10 },
      ],
      speed: 500, randomPick: true,
    },
    hit:    { strips: [{ src: '/sprites/CLERIC/Hurt.png', frames: 2 }], speed: 400 },
    death:  { strips: [{ src: '/sprites/CLERIC/Dead.png', frames: 5 }], speed: 600 },
  },
};

export const AVATARS: SpriteAvatar[] = [
  // WARRIOR
  { id: 'warrior_1', name: '검사',       row: 0, classKey: 'WARRIOR', sprite: SWORDMAN },
  { id: 'warrior_2', name: '대검 여전사', row: 0, classKey: 'WARRIOR', sprite: GIANT_SWORD },
  { id: 'warrior_3', name: '기사',       row: 0, classKey: 'WARRIOR', sprite: KNIGHT },
  { id: 'warrior_4', name: '전사',       row: 0, classKey: 'WARRIOR', sprite: WORRIOR_SPRITE },
  { id: 'warrior_5', name: '여기사',     row: 0, classKey: 'WARRIOR', sprite: WOMAN_KNIGHT_SPRITE },
  // MAGE
  { id: 'mage_1',    name: '마법사',     row: 0, classKey: 'MAGE',    sprite: MAGE_SPRITE },
  // RANGER
  { id: 'ranger_1',  name: '레인저',     row: 0, classKey: 'RANGER',  sprite: RANGER_SPRITE },
  { id: 'ranger_2',  name: '사냥꾼',     row: 0, classKey: 'RANGER',  sprite: RANGER_SPRITE },
  // ROGUE
  { id: 'rogue_1',   name: '닌자',       row: 0, classKey: 'ROGUE',   sprite: NINJA },
  { id: 'rogue_2',   name: '해적',       row: 0, classKey: 'ROGUE',   sprite: PIRATE },
  { id: 'rogue_3',   name: '도적',       row: 0, classKey: 'ROGUE',   sprite: ROGUE_SPRITE },
  // CLERIC
  { id: 'cleric_1',  name: '성직자',     row: 0, classKey: 'CLERIC',  sprite: CLERIC_SPRITE },
];

export function getAvatarById(id: string | null): SpriteAvatar | undefined {
  if (!id) return undefined;
  return AVATARS.find(a => a.id === id);
}

export function getAvatarsForClass(classKey: string): SpriteAvatar[] {
  return AVATARS.filter(a => a.classKey === classKey);
}
