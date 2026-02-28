import { useEffect, useState } from 'react';
import { SPRITE, getAvatarById } from '../data/avatars';

type AnimationType = 'idle' | 'walk' | 'attack' | 'hit' | 'death';

interface SpriteAvatarProps {
  avatarId: string | null;
  animation?: AnimationType;
  scale?: number;
  flip?: boolean;
  paused?: boolean;
  frozenFrame?: number;
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

  const sprite = avatar?.sprite || SPRITE;
  const anim = sprite.animations[animation] || sprite.animations.idle;
  const s = scale * sprite.baseScale;

  useEffect(() => {
    if (paused) {
      if (frozenFrame !== undefined) {
        setFrame(Math.min(frozenFrame, anim.frames - 1));
      }
      return;
    }
    setFrame(0);
    const interval = setInterval(() => {
      setFrame(prev => (prev + 1) % anim.frames);
    }, anim.speed / anim.frames);
    return () => clearInterval(interval);
  }, [animation, anim.frames, anim.speed, paused, frozenFrame]);

  if (!avatar) {
    return <div style={{ width: 48 * scale, height: 48 * scale, background: '#333', borderRadius: 4 }} />;
  }

  const col = anim.startCol + frame;
  const row = avatar.row;
  const bgX = -(col * sprite.cellWidth);
  const bgY = -(row * sprite.cellHeight);
  const displayW = sprite.cellWidth * s;
  const displayH = sprite.cellHeight * s;

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
