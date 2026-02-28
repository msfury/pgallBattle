import { useEffect, useState } from 'react';
import { SPRITE, getAvatarById } from '../data/avatars';

type AnimationType = 'idle' | 'walk' | 'attack' | 'hit' | 'death';

interface SpriteAvatarProps {
  avatarId: string | null;
  animation?: AnimationType;
  scale?: number;
  flip?: boolean;
  paused?: boolean;
}

export default function SpriteAvatar({
  avatarId,
  animation = 'idle',
  scale = 1,
  flip = false,
  paused = false,
}: SpriteAvatarProps) {
  const avatar = getAvatarById(avatarId);
  const [frame, setFrame] = useState(0);

  const anim = SPRITE.animations[animation] || SPRITE.animations.idle;

  useEffect(() => {
    if (paused) return;
    setFrame(0);
    const interval = setInterval(() => {
      setFrame(prev => (prev + 1) % anim.frames);
    }, anim.speed / anim.frames);
    return () => clearInterval(interval);
  }, [animation, anim.frames, anim.speed, paused]);

  if (!avatar) {
    return <div style={{ width: 48 * scale, height: 48 * scale, background: '#333', borderRadius: 4 }} />;
  }

  const col = anim.startCol + frame;
  const row = avatar.row;
  const bgX = -(col * SPRITE.cellWidth);
  const bgY = -(row * SPRITE.cellHeight);
  const displayW = SPRITE.cellWidth * scale;
  const displayH = SPRITE.cellHeight * scale;

  return (
    <div
      style={{
        width: displayW,
        height: displayH,
        backgroundImage: `url(${SPRITE.src})`,
        backgroundPosition: `${bgX * scale}px ${bgY * scale}px`,
        backgroundSize: `${SPRITE.sheetWidth * scale}px ${SPRITE.sheetHeight * scale}px`,
        backgroundRepeat: 'no-repeat',
        imageRendering: 'pixelated',
        transform: flip ? 'scaleX(-1)' : undefined,
      }}
    />
  );
}
