# pgallBattle - D&D 스타일 캐릭터 배틀 게임

캐릭터를 생성하고, 장비를 가챠로 뽑고, 상점에서 전투 아이템을 구매하고, 다른 캐릭터와 D&D 룰 기반 전투를 하는 모바일 웹 게임.

---

## 빠른 시작

### API 서버
```bash
cd api
./gradlew bootRun          # 실행 (http://localhost:8080)
# 종료: Ctrl+C (포그라운드) 또는 터미널 닫기
```

### React 개발서버
```bash
cd web
npm install                # 최초 1회
npm run dev                # 실행 (http://localhost:5173)
# 종료: Ctrl+C (포그라운드) 또는 터미널 닫기
```

### 서버 강제 종료 (포트로 찾기)
```bash
# Windows
netstat -ano | findstr :8080     # PID 확인
taskkill /PID <PID번호> /F       # API 서버 종료

netstat -ano | findstr :5173     # PID 확인
taskkill /PID <PID번호> /F       # React 서버 종료
```

---

## 기술 스택

| 구분 | 기술 | 버전 |
|------|------|------|
| API 서버 | Spring Boot | 4.0.3 |
| 언어 (API) | Java (Amazon Corretto) | 25 |
| 빌드 도구 | Gradle | 9.3.1 |
| ORM | JPA / Hibernate | 7.2.4 |
| 보일러플레이트 | Lombok | 1.18.40 |
| 데이터베이스 | PostgreSQL | 17 |
| 프론트엔드 | React + TypeScript | 18 |
| 번들러 | Vite | 7.3 |
| 라우팅 | React Router | 7.x |

---

## 프로젝트 구조

```
pgallBattle/
├── api/                                    # Spring Boot API 서버
│   ├── build.gradle
│   ├── settings.gradle
│   ├── gradlew / gradlew.bat
│   └── src/main/java/com/pgall/battle/
│       ├── PgallBattleApplication.java     # 메인 엔트리
│       ├── config/
│       │   ├── CorsConfig.java             # CORS 설정
│       │   └── GlobalExceptionHandler.java # 전역 에러 핸들러
│       ├── entity/
│       │   ├── GameCharacter.java          # 캐릭터
│       │   ├── Equipment.java              # 장비 (가챠)
│       │   ├── ShopItem.java               # 상점 아이템
│       │   ├── Inventory.java              # 캐릭터 보유 아이템
│       │   └── BattleLog.java              # 전투 기록
│       ├── enums/
│       │   ├── EquipmentType.java          # WEAPON, HELMET, ARMOR, SHOES, EARRING, RING
│       │   ├── EquipmentGrade.java         # COMMON ~ LEGENDARY
│       │   ├── EquipmentEffect.java        # 장비 특수효과
│       │   └── BuffType.java               # 상점 아이템 버프
│       ├── repository/                     # JPA Repository (5개)
│       ├── service/
│       │   ├── CharacterService.java       # 캐릭터 생성/조회, 스탯 롤링
│       │   ├── GachaService.java           # 장비 가챠 (등급별 확률)
│       │   ├── ShopService.java            # 상점 아이템 구매
│       │   └── BattleService.java          # D&D 전투 시스템
│       ├── controller/                     # REST Controller (4개)
│       └── dto/                            # Request/Response DTO (7개)
│
└── web/                                    # React 프론트엔드
    ├── package.json
    ├── vite.config.ts                      # Vite + API 프록시 설정
    └── src/
        ├── App.tsx                         # 라우팅 설정
        ├── main.tsx
        ├── index.css                       # 모바일 최적화 다크 테마
        ├── api/
        │   └── client.ts                   # API 클라이언트 모듈
        └── pages/
            ├── CharacterCreatePage.tsx      # 스탯 랜덤 굴리기 + 캐릭터 생성
            ├── MainPage.tsx                 # 내 캐릭터 정보, 캐릭터 목록, 상점/가챠 버튼
            ├── CharacterDetailPage.tsx      # 캐릭터 상세 + 전투 도전
            ├── ShopPage.tsx                 # 상점 (전투 버프 아이템)
            ├── GachaPage.tsx                # 장비 가챠 뽑기
            └── BattlePage.tsx              # 전투 진행 + 로그 표시
```

---

## 실행 방법

### 사전 요구사항

- Java 25 (Amazon Corretto)
- Node.js 24+
- PostgreSQL 17

### 1. 데이터베이스 준비

```bash
# PostgreSQL에 접속하여 유저 및 DB 생성
psql -U postgres
CREATE ROLE pgall WITH LOGIN PASSWORD 'pgall1234!@#$' CREATEDB;
CREATE DATABASE pgallbattle OWNER pgall;
```

### 2. API 서버 실행

```bash
cd api
./gradlew bootRun
```

서버가 `http://localhost:8080`에서 실행됩니다.
최초 실행 시 JPA가 테이블을 자동 생성하고, `data.sql`로 상점 초기 데이터를 삽입합니다.

### 3. React 개발서버 실행

```bash
cd web
npm install    # 최초 1회
npm run dev
```

`http://localhost:5173`에서 접속 가능합니다.
Vite 프록시가 `/api/**` 요청을 `localhost:8080`으로 전달합니다.

---

## API 엔드포인트

### Character
| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET | `/api/characters/random-stats` | 랜덤 스탯 생성 (4d6 drop lowest) |
| POST | `/api/characters` | 캐릭터 생성 |
| GET | `/api/characters` | 전체 캐릭터 목록 |
| GET | `/api/characters/{id}` | 캐릭터 상세 조회 |

### Gacha
| 메서드 | 경로 | 설명 |
|--------|------|------|
| POST | `/api/gacha/{characterId}` | 가챠 뽑기 (30G 소모) |

### Shop
| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET | `/api/shop/items` | 상점 아이템 목록 |
| POST | `/api/shop/buy` | 아이템 구매 |

### Battle
| 메서드 | 경로 | 설명 |
|--------|------|------|
| POST | `/api/battle` | 전투 실행 |
| GET | `/api/battle/logs/{characterId}` | 전투 기록 조회 |

---

## 게임 시스템

### 캐릭터 생성
- D&D 방식 **4d6 drop lowest**로 6개 스탯(STR, DEX, CON, INT, WIS, CHA)을 랜덤 생성
- 마음에 드는 스탯이 나올 때까지 무한 재굴림 가능
- 초기 골드 100G 지급
- HP = 10 + CON 보정치

### 가챠 (장비 뽑기)
- **비용**: 30G
- **장비 종류**: 무기, 투구, 갑옷, 신발, 귀걸이, 반지
- **등급별 확률**:

| 등급 | 확률 | 특수효과 |
|------|------|----------|
| Common | 49% | 없음 |
| Uncommon | 30% | 없음 |
| Rare | 15% | 60% 확률로 부여 |
| Epic | 5% | 60% 확률로 부여 |
| Legendary | 1% | 60% 확률로 부여 |

### 장비 특수효과

| 효과 | 설명 | 부여 가능 장비 |
|------|------|----------------|
| DOUBLE_ATTACK | 공격을 두 번 수행 | 무기 |
| LIFE_STEAL | 데미지의 일부를 HP로 흡수 | 무기 |
| STUN | 상대를 기절시켜 턴 스킵 | 투구 |
| BLOCK_CHANCE | 상대 공격을 차단 | 갑옷 |
| DEBUFF_DEF_DOWN | 상대 방어력 감소 | 신발 |
| DEBUFF_ATK_DOWN | 상대 공격력 감소 | 귀걸이 |
| POISON | 매 턴 독 데미지 | 반지 |

### 상점

| 아이템 | 가격 | 효과 |
|--------|------|------|
| 크리티컬 부적 | 50G | 크리티컬 범위 19~20으로 확장 |
| 질풍의 두루마리 | 80G | 공격을 두 번 수행 |
| 수호의 방패 | 40G | 공격을 한 번 완전히 차단 |
| 치유 물약 | 30G | 전투 시작 전 HP 회복 |

### 전투 시스템 (D&D 간소화)

1. **이니셔티브**: 양측 d20 + DEX 보정치로 선공 결정
2. **공격 판정**: d20 + STR 보정치 >= 상대 AC(10 + DEX 보정치 + 장비 방어력)
3. **데미지**: 무기 기본 데미지 + STR 보정치
4. **크리티컬**: d20이 20이면 데미지 2배 (크리티컬 부적 사용 시 19~20)
5. **버프/특수효과**: 전투 시작 시 보유 아이템 및 장비 효과 발동 판정
6. **승리 조건**: 상대 HP를 0 이하로 만들거나, 20라운드 후 HP가 더 많은 쪽 판정승
7. **보상**: 승리 시 10~29G 랜덤 획득

---

## 환경 설정 참고

### application.yml (API 서버)
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/pgallbattle
    username: pgall
    password: 'pgall1234!@#$'
```

### 환경 변수
```
JAVA_HOME=C:\DEV\java\Amazon Corretto\jdk25.0.2
PATH에 추가: C:\DEV\java\Amazon Corretto\jdk25.0.2\bin, C:\DEV\nodejs
```
