import { useEffect, useRef, useState } from 'react';
import { getAvatarById, isStripSprite, type StripSpriteConfig } from '../data/avatars';

export type AnimationType = 'idle' | 'walk' | 'attack' | 'magicAttack' | 'hit' | 'death';

interface SpriteAvatarProps {
  avatarId: string | null;
  animation?: AnimationType;
  scale?: number;
  flip?: boolean;
  paused?: boolean;
  frozenFrame?: number;
}

/** 멀티-스트립 애니메이션에서 현재 프레임의 소스와 컬럼을 계산 */
function resolveStripFrame(
  strips: { src: string; frames: number }[],
  frameIndex: number,
  cellWidth: number,
): { src: string; col: number; stripWidth: number } {
  let remaining = frameIndex;
  for (const strip of strips) {
    if (remaining < strip.frames) {
      return { src: strip.src, col: remaining, stripWidth: strip.frames * cellWidth };
    }
    remaining -= strip.frames;
  }
  const last = strips[strips.length - 1];
  return { src: last.src, col: last.frames - 1, stripWidth: last.frames * cellWidth };
}

export default function SpriteAvatar({
  avatarId,
  animation = 'idle',
  scale = 1,
  flip = false,
  paused = false,
  frozenFrame,
}: SpriteAvatarProps) {
  const avatar = getAvatarById(avatarId);
  const [frame, setFrame] = useState(0);

  if (!avatar) {
    return <div style={{ width: 48 * scale, height: 48 * scale, background: '#333', borderRadius: 4 }} />;
  }

  const sprite = avatar.sprite;
  const strip = isStripSprite(sprite);
  const s = scale * sprite.baseScale;

  // ── 스트립 이미지 프리로드 ──
  useEffect(() => {
    if (!strip) return;
    const srcs = new Set<string>();
    Object.values((sprite as StripSpriteConfig).animations).forEach(anim => {
      anim.strips.forEach(st => srcs.add(st.src));
    });
    srcs.forEach(src => { new Image().src = src; });
  }, [sprite, strip]);

  // ── randomPick: 애니메이션 전환 시 랜덤 스트립 선택 ──
  const prevAnimRef = useRef(animation);
  const pickIdxRef = useRef(0);

  if (prevAnimRef.current !== animation) {
    prevAnimRef.current = animation;
    if (strip) {
      const sp = sprite as StripSpriteConfig;
      const anim = sp.animations[animation]
        || (animation === 'magicAttack' ? sp.animations['attack'] : undefined)
        || sp.animations.idle;
      if (anim.randomPick && anim.strips.length > 1) {
        pickIdxRef.current = Math.floor(Math.random() * anim.strips.length);
      }
    }
  }

  // ── 애니메이션 해상도 계산 ──
  let totalFrames: number;
  let animSpeed: number;

  if (strip) {
    const sp = sprite as StripSpriteConfig;
    const anim = sp.animations[animation]
      || (animation === 'magicAttack' ? sp.animations['attack'] : undefined)
      || sp.animations.idle;
    if (anim.randomPick) {
      const picked = anim.strips[pickIdxRef.current % anim.strips.length];
      totalFrames = picked.frames;
    } else {
      totalFrames = anim.strips.reduce((sum, st) => sum + st.frames, 0);
    }
    animSpeed = anim.speed;
  } else {
    const anim = sprite.animations[animation] || sprite.animations.idle;
    totalFrames = anim.frames;
    animSpeed = anim.speed;
  }

  useEffect(() => {
    if (paused) {
      if (frozenFrame !== undefined) {
        setFrame(Math.min(frozenFrame, totalFrames - 1));
      }
      return;
    }
    setFrame(0);
    const interval = setInterval(() => {
      setFrame(prev => (prev + 1) % totalFrames);
    }, animSpeed / totalFrames);
    return () => clearInterval(interval);
  }, [animation, totalFrames, animSpeed, paused, frozenFrame]);

  const displayW = sprite.cellWidth * s;
  const displayH = sprite.cellHeight * s;

  // ── 스트립 스프라이트 렌더링 ──
  if (strip) {
    const sp = sprite as StripSpriteConfig;
    const anim = sp.animations[animation]
      || (animation === 'magicAttack' ? sp.animations['attack'] : undefined)
      || sp.animations.idle;

    let src: string;
    let col: number;
    let stripWidth: number;

    if (anim.randomPick) {
      const picked = anim.strips[pickIdxRef.current % anim.strips.length];
      src = picked.src;
      col = frame;
      stripWidth = picked.frames * sp.cellWidth;
    } else {
      ({ src, col, stripWidth } = resolveStripFrame(anim.strips, frame, sp.cellWidth));
    }

    const bgX = -(col * sp.cellWidth);
    return (
      <div
        style={{
          width: displayW,
          height: displayH,
          backgroundImage: `url(${src})`,
          backgroundPosition: `${bgX * s}px 0px`,
          backgroundSize: `${stripWidth * s}px ${sp.cellHeight * s}px`,
          backgroundRepeat: 'no-repeat',
          imageRendering: 'pixelated',
          transform: flip ? 'scaleX(-1)' : undefined,
        }}
      />
    );
  }

  // ── 기존 스프라이트시트 렌더링 ──
  const anim = sprite.animations[animation] || sprite.animations.idle;
  const col = anim.startCol + frame;
  const row = avatar.row;
  const bgX = -(col * sprite.cellWidth);
  const bgY = -(row * sprite.cellHeight);

  return (
    <div
      style={{
        width: displayW,
        height: displayH,
        backgroundImage: `url(${sprite.src})`,
        backgroundPosition: `${bgX * s}px ${bgY * s}px`,
        backgroundSize: `${sprite.sheetWidth * s}px ${sprite.sheetHeight * s}px`,
        backgroundRepeat: 'no-repeat',
        imageRendering: 'pixelated',
        transform: flip ? 'scaleX(-1)' : undefined,
      }}
    />
  );
}
