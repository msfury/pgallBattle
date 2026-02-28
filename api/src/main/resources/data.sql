-- 기존 효과명을 새 enum 이름으로 마이그레이션
UPDATE equipment SET effect = 'STUN_STRIKE' WHERE effect = 'STUN';
UPDATE equipment SET effect = 'FIRE_DAMAGE' WHERE effect = 'FIRE';
UPDATE equipment SET effect = 'ICE_DAMAGE' WHERE effect = 'ICE';
UPDATE equipment SET effect = 'LIGHTNING_DAMAGE' WHERE effect = 'LIGHTNING';
