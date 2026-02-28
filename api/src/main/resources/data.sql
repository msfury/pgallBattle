INSERT INTO shop_item (name, description, price, buff_type, buff_chance)
SELECT '크리티컬 부적', '전투 시 크리티컬 범위가 19~20으로 확장됩니다.', 50, 'CRIT_DOUBLE', 100
WHERE NOT EXISTS (SELECT 1 FROM shop_item WHERE name = '크리티컬 부적');

INSERT INTO shop_item (name, description, price, buff_type, buff_chance)
SELECT '질풍의 두루마리', '전투 시 공격을 두 번 수행합니다.', 80, 'DOUBLE_ATTACK', 100
WHERE NOT EXISTS (SELECT 1 FROM shop_item WHERE name = '질풍의 두루마리');

INSERT INTO shop_item (name, description, price, buff_type, buff_chance)
SELECT '수호의 방패', '전투 시 한 번 공격을 완전히 막습니다.', 40, 'SHIELD', 100
WHERE NOT EXISTS (SELECT 1 FROM shop_item WHERE name = '수호의 방패');

INSERT INTO shop_item (name, description, price, buff_type, buff_chance)
SELECT '치유 물약', '전투 시작 전 HP를 회복합니다.', 30, 'HEAL', 100
WHERE NOT EXISTS (SELECT 1 FROM shop_item WHERE name = '치유 물약');
