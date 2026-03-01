# pgallBattle

D&D 5e 룰 기반 캐릭터 배틀 웹게임. 캐릭터를 생성하고, 장비를 가챠로 뽑고, 무기를 강화하고, 물약을 구매해서 다른 플레이어와 PvP 전투를 벌인다.

---

## 기술 스택

| 구분 | 기술 |
|------|------|
| API 서버 | Spring Boot 4.0.3 / Java 21 |
| 빌드 | Gradle 9.3.1 |
| ORM | JPA / Hibernate 7.2 |
| DB | SQLite (WAL mode) |
| 프론트엔드 | React 19 + TypeScript 5.9 |
| 번들러 | Vite 7.3 |
| 라우팅 | React Router 7.x |
| 배포 | Docker Compose |

---

## 프로젝트 구조

```
pgallBattle/
├── api/                                     # Spring Boot API
│   ├── Dockerfile
│   ├── build.gradle
│   └── src/main/java/com/pgall/battle/
│       ├── config/                          # CORS, Security, 예외 핸들러, DB 마이그레이션
│       ├── entity/                          # GameCharacter, Equipment, EnhanceEffect, Inventory, ShopItem, BattleLog
│       ├── enums/                           # CharacterClass, EquipmentType/Grade/Effect, BuffType, WeaponCategory, ScalingStat
│       ├── repository/                      # JPA Repository
│       ├── service/
│       │   ├── CharacterService.java        # 캐릭터 CRUD, 스탯 롤링(총합 72제한), 일급
│       │   ├── GachaService.java            # 장비 가챠
│       │   ├── ShopService.java             # 물약 상점
│       │   ├── EquipService.java            # 장비/물약 장착·해제·판매
│       │   ├── EnhanceService.java          # 무기 강화 시스템
│       │   ├── BattleService.java           # D&D 전투 엔진 (60종 효과)
│       │   ├── HeroService.java             # NPC 용사 시스템
│       │   └── DailyScheduleService.java    # 매일 0시 스케줄
│       ├── controller/                      # REST 엔드포인트
│       ├── dto/                             # 요청/응답 DTO
│       └── filter/
│           └── IpOwnershipFilter.java       # IP 기반 소유권 검증
│
├── web/                                     # React 프론트엔드
│   ├── Dockerfile
│   ├── nginx.conf
│   └── src/
│       ├── api/client.ts                    # API 클라이언트 (타입 안전)
│       ├── components/
│       │   ├── BattleArena.tsx              # 전투 시각화 (HP바, 물약, 로그)
│       │   └── SpriteAvatar.tsx             # 스프라이트 아바타
│       ├── data/                            # 아바타, 클래스 데이터
│       └── pages/
│           ├── HomePage.tsx                 # ELO 랭킹 + 전투 버튼
│           ├── CharacterCreatePage.tsx      # 캐릭터 생성
│           ├── MyPage.tsx                   # 캐릭터 정보 + 장비/물약/강화 관리
│           ├── ShopPage.tsx                 # 물약 상점
│           ├── GachaPage.tsx                # 장비 가챠
│           └── BattlePage.tsx               # 전투 실행
│
└── docker-compose.yml
```

---

## 실행 방법

### 사전 요구사항
- Java 21
- Node.js 22+

### API 서버
```bash
cd api
./gradlew bootRun    # http://localhost:8080
```

최초 실행 시 JPA가 테이블을 자동 생성하고, NPC 용사 캐릭터 5명이 생성된다.
`DatabaseMigration` 컴포넌트가 시작 시 누락된 컬럼/테이블을 자동 추가한다.

### 프론트엔드 개발서버
```bash
cd web
npm install          # 최초 1회
npm run dev          # http://localhost:5173
```

Vite 프록시가 `/api/*` 요청을 `localhost:8080`으로 전달한다.

### Docker 배포
```bash
docker-compose up --build    # API: 8080, Web: 80
```

SQLite DB는 Docker named volume(`pgall-data`)에 저장되어 컨테이너 재시작 시에도 유지된다.

---

## API 엔드포인트

### 캐릭터
| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET | `/api/characters/random-stats` | 4d6 drop lowest 스탯 생성 |
| POST | `/api/characters` | 캐릭터 생성 |
| GET | `/api/characters/ranking` | ELO 랭킹 조회 |
| GET | `/api/characters/{id}` | 캐릭터 상세 |
| GET | `/api/characters/mine` | 내 캐릭터 조회 (IP 기반) |
| DELETE | `/api/characters/{id}` | 캐릭터 삭제 |
| POST | `/api/characters/{id}/daily-check` | 일급 300G 수령 |

### 장비
| 메서드 | 경로 | 설명 |
|--------|------|------|
| POST | `/api/gacha/{characterId}` | 가챠 뽑기 (30G) |
| PUT | `/api/characters/{charId}/equipment/{equipId}/equip` | 장착 |
| PUT | `/api/characters/{charId}/equipment/{equipId}/unequip` | 해제 |
| POST | `/api/characters/{charId}/equipment/{equipId}/sell` | 판매 |
| POST | `/api/characters/{charId}/equipment/{equipId}/enhance` | 무기 강화 |
| GET | `/api/characters/{charId}/equipment/{equipId}/enhance-info` | 강화 정보 조회 |

### 물약
| 메서드 | 경로 | 설명 |
|--------|------|------|
| PUT | `/api/characters/{charId}/potion/{invId}/equip` | 물약 장착 (최대 5개) |
| PUT | `/api/characters/{charId}/potion/{invId}/unequip` | 물약 해제 |
| POST | `/api/characters/{charId}/potion/{invId}/sell` | 물약 판매 (구매가의 50%) |

### 상점
| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET | `/api/shop/{characterId}/items` | 상점 목록 |
| POST | `/api/shop/{characterId}/refresh` | 상점 새로고침 (골드 소모) |
| POST | `/api/shop/{characterId}/buy/{index}` | 물약 구매 |

### 전투
| 메서드 | 경로 | 설명 |
|--------|------|------|
| POST | `/api/battle` | PvP 전투 실행 |
| GET | `/api/battle/logs/{characterId}` | 전투 기록 |

---

## 게임 시스템

### 캐릭터
- **클래스**: 전사(STR), 도적(DEX), 마법사(INT), 성직자(WIS), 궁수(DEX)
- **스탯**: 4d6 drop lowest 방식, 총합 72 제한 (0.01% 확률로 올스탯 20)
- **HP**: 클래스별 Hit Die(d6~d12) + CON 보정치
- **초기 골드**: 100G / **일급**: 매일 300G
- **ELO**: 1000에서 시작, K=32 로지스틱 곡선
- **소유권**: IP당 캐릭터 1개

### 장비 가챠

**비용**: 30G / **장비 종류**: 무기, 투구, 갑옷, 장갑, 신발, 귀걸이, 반지

| 등급 | 확률 | 특수효과 확률 | 판매가 |
|------|------|--------------|--------|
| Common | 49% | - | 5G |
| Uncommon | 30% | - | 10G |
| Rare | 15% | 60% | 20G |
| Epic | 5% | 80% | 100G |
| Legendary | 1% | 100% | 200G |

**장착 슬롯 (10칸)**: 무기 x2, 투구 x1, 갑옷 x1, 장갑 x1, 신발 x1, 귀걸이 x2, 반지 x2

**무기 카테고리**: 양손(지팡이, 창, 대검, 활) / 한손(검, 단검, 클로, 메이스, 도끼, 레이피어, 완드, 도리깨)
- 양손무기는 무기 슬롯 2개를 모두 차지
- 한손무기는 듀얼 와일드 가능

### 무기 강화

무기에 골드를 소모하여 강화 수치를 올린다. 실패 시 확률적으로 무기가 파괴될 수 있다.

| 구간 | 비용 | 성공률 | 파괴 확률 (실패 시) |
|------|------|--------|-------------------|
| +1 ~ +3 | 5G | 80% | 0% |
| +4 ~ +6 | 10G | 60% | 5% |
| +7 ~ +9 | 25G | 50% | 10% |
| +10 이상 | 100G | 30% | 15% |

**스탯 보너스** (피보나치형, +3 마일스톤): +3→ATK/DMG+1, +6→+3, +9→+5, +12→+8, +15→+13, +18→+21...

**랜덤 효과 부여**: +6→1개, +9→3개, +12→5개 (무기 계열 효과 중 랜덤)

### 물약 시스템

- **보관**: 무제한 / **장착 슬롯**: 5개 (전투에 활성화)
- **판매가**: 구매가의 50%
- **상점**: 랜덤 4~6종 진열, 새로고침 (비용 점증)

| 종류 | 물약 |
|------|------|
| 치유 | 치유 물약, 대형 치유 물약 |
| 공격 | 크리티컬 더블, 더블 어택, 화염/빙결/번개/신성 부여, 관통 강화 |
| 방어 | 보호 물약, 재생 물약, 철피 물약 |
| 유틸 | 반사 물약, 명중 물약, 가속 물약, 축복 물약 |

### 특수효과 (60종)

**무기 효과** (20종): 화염/빙결/번개/신성/암흑/산성 공격, 관통, 출혈, 흡혈, 더블 어택, 크리티컬 강화, 기절 타격, 넉백, 참수, 공격력 감소, 방어력 감소, 속도 감소, 침묵, 무장 해제, 처형

**방어구 효과** (20종): 공격 차단, 마법 저항, 가시, HP 재생, 피해 감소, 회피 증가, 5종 원소 저항, 기절 저항, 마법 반사, 재기, 중갑, 인내, 불굴, 철피, 치유 오라, 흡수 보호막, 강화

**악세사리 효과** (20종): 명중률 증가, 반격, 독, 허약 저주, 마나 보호막, 가속, 행운, 흡혈 오라, 죽음의 보호, 위협, 축복, 완전 회피, 꿰뚫는 시선, 영혼 수확, 비전 집중, 신의 은총, 혼돈 일격, 원소 강화, 영혼 연결, 마나 흡수

### 전투 (D&D 5e 기반)

1. **이니셔티브**: d20 + DEX 보정치 → 선공 결정
2. **공격 판정**: d20 + 능력치 보정 ≥ 상대 AC
3. **AC**: 10 + DEX 보정치 + 장비 방어력 + 클래스 보너스
4. **데미지**: 무기 기본 데미지 + 스케일링 스탯 보정치
   - STR 무기: `mod(STR) + mod(DEX)`
   - DEX/INT/WIS 무기: `mod(해당 스탯) + 2`
5. **크리티컬**: d20=20 → 데미지 2배 (크리티컬 물약 시 19~20)
6. **장비 효과**: 공격/피격 시 확률적으로 발동 (강화 효과 포함)
7. **물약**: 전투 시작 시 자동 적용, 힐 물약은 HP 25% 이하 시 자동 사용
8. **승리 조건**: 상대 HP 0 이하 또는 20라운드 후 HP 비교
9. **골드 보상**: ELO 티어별 차등 (1500+: 30~39G, 1200+: 20~29G, 기본: 10~19G) + 상위 ELO 격파 보너스

### NPC 용사 시스템

- 서버 시작 시 클래스별 1명씩 5명의 용사 자동 생성
- **능력치**: 클래스에 맞는 주요 스탯 최고치, 총합 100 이상
- **장비**: 초기 50회 가챠 (RARE 이상 보장) + 클래스에 맞는 무기 자동 장착
- 매일 0시 5회 가챠 추가 → 장비 갱신
- 랭킹에 참여하여 다른 플레이어가 도전 가능

---

## 프론트엔드 라우트

| 경로 | 페이지 | 설명 |
|------|--------|------|
| `/` | 홈 | ELO 랭킹 테이블, 전투 상대 선택 |
| `/create` | 캐릭터 생성 | 스탯 롤링, 클래스·아바타 선택 |
| `/mypage/:id` | 내 캐릭터 | 장비 슬롯, 물약 슬롯(5칸), 강화, 인벤토리 |
| `/character/:id` | 캐릭터 정보 | 다른 플레이어 캐릭터 조회 |
| `/shop/:id` | 물약 상점 | 물약 구매·새로고침 |
| `/gacha/:id` | 장비 가챠 | 장비 뽑기 + 결과 표시 |
| `/battle/:attackerId/:defenderId` | 전투 | 전투 시뮬레이션 + 로그 |
