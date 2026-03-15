# 📈 InvestSim — 전략 기반 암호화폐 모의 자동매매 시스템

> 실시간 시세 데이터를 기반으로 사용자가 직접 설계한 투자 전략의 수익성을 검증하는 모의 자동매매 백엔드 플랫폼

![Java](https://img.shields.io/badge/Java-17-blue)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.x-green)
![MySQL](https://img.shields.io/badge/MySQL-8.x-orange)
![Redis](https://img.shields.io/badge/Redis-7.x-red)
![JWT](https://img.shields.io/badge/JWT-Auth-purple)
![WebSocket](https://img.shields.io/badge/WebSocket-Upbit-red)
![Docker](https://img.shields.io/badge/Docker-Compose-blue)
![AWS](https://img.shields.io/badge/AWS-EC2%20%7C%20S3%20%7C%20CloudFront-orange)
![Status](https://img.shields.io/badge/Status-Deployed-brightgreen)

---

## 목차
- [프로젝트 개요](#-프로젝트-개요)
- [기술 스택](#-기술-스택)
- [아키텍처](#-아키텍처)
- [실시간 데이터 흐름](#-실시간-데이터-흐름)
- [핵심 설계 포인트](#-핵심-설계-포인트)
- [도메인 모델](#-도메인-모델)
- [API 명세](#-api-명세)

---

## 📌 프로젝트 개요

InvestSim은 업비트(Upbit) 거래소의 실시간 체결 데이터를 WebSocket으로 수신하고,
사용자가 직접 설정한 보조지표 기반 전략 조건에 따라 모의 매수/매도를 자동 실행하는 백엔드 시스템입니다.

실제 자산을 사용하지 않고 전략의 수익성과 유효성을 검증하는 것이 핵심 목적입니다.

### 기획 의도
- 단순 CRUD를 넘어 **실시간 데이터 처리 + 자동화 로직**을 구현하는 경험
- **JPA 심화** (N+1 해결, QueryDSL 동적 쿼리, 낙관적 락)를 실전에서 적용
- **헥사고날 아키텍처** 기반으로 계층 간 의존성을 제어하는 설계 경험

---

## 🛠 기술 스택

| 구분 | 기술 |
|------|------|
| Language | Java 17 |
| Framework | Spring Boot 3.x, Spring Data JPA, Spring Security |
| Query | QueryDSL 5.x |
| Database | MySQL 8.x (Docker), H2 (테스트) |
| Cache | Redis 7.x (Docker) |
| Auth | JWT (jjwt) + Redis (Refresh Token) |
| Real-time | WebSocket (Upbit API) |
| Docs | Swagger (SpringDoc) |
| Test | JUnit 5, H2 In-Memory |
| Infra | AWS EC2 (t3.micro), S3, CloudFront, Docker Compose, Nginx, Let's Encrypt |
| Tools | IntelliJ IDEA, DBeaver, Gradle |

---

## 🏗 아키텍처

**헥사고날 아키텍처 (Ports & Adapters)** 를 적용해 Application 계층이 외부(웹, DB, WebSocket)에 의존하지 않도록 설계했습니다.

```
[인바운드 어댑터]                [애플리케이션]                   [아웃바운드 어댑터]

StrategyController  ──>  StrategyUseCase (in port)
TradeController     ──>  TradeUseCase (in port)         StrategyPort (out port)  ──>  StrategyPersistenceAdapter
MemberController    ──>  MemberUseCase (in port)   ──>  TradePort (out port)     ──>  TradePersistenceAdapter
TickProcessor       ──>       ↓                         MemberPort (out port)    ──>  MemberPersistenceAdapter
                       StrategyService                  CurrentPricePort         ──>  CurrentPriceStore
                       TradeService
                       MemberService
```

| 계층 | 패키지 | 역할 |
|------|--------|------|
| Domain | `domain/model`, `domain/service` | 핵심 엔티티 및 도메인 로직 |
| Application | `application/service`, `application/port` | UseCase 정의, 포트 인터페이스 |
| Inbound Adapter | `adapter/in/web`, `adapter/in/websocket`, `adapter/in/auth` | REST API, WebSocket, JWT |
| Outbound Adapter | `adapter/out/persistence`, `adapter/out/upbit` | JPA, Upbit REST 클라이언트 |

**의존성 방향:** 항상 바깥 → 안쪽. Application 계층은 어댑터를 전혀 모릅니다.

---

## 🔄 실시간 데이터 흐름

```
[Upbit WebSocket]
       |
       | 체결 틱 수신 (TradeTickData)
       v
[UpbitWebSocketClient]  ──(재연결: 지수 백오프)──>  [재연결]
       |
       v
[TickProcessor]
       |
       |── CurrentPriceStore 업데이트 (ConcurrentHashMap)
       |── CandleStore에서 캔들 윈도우 조회 (최대 50개, 슬라이딩 윈도우)
       |── QueryDSL로 활성 전략 조회 (findActiveStrategiesByMarket)
       |
       v
[StrategyEvaluator]  ──>  [IndicatorCalculator]
       |                        |── RSI (EMA 방식)
       |                        |── VOLUME
       |                        └── MA (단순 이동평균)
       |
       | BUY / SELL / HOLD 신호
       v
[TradeService]
       |── BUY  → Trade 저장 (openAmount, openPricePerShare, openQuantity)
       └── SELL → Trade 청산 (closeAmount, profitAmount, profitRate 계산)
```

```
[Client]
   |
   | REST API (JWT 인증)
   v
[Controller]  ──>  [UseCase (in port)]  ──>  [Service]  ──>  [Port (out port)]  ──>  [MySQL]
```

---

## 💡 핵심 설계 포인트

### 1. N+1 문제 해결 — @EntityGraph / @BatchSize 적용

**Trade 조회** (`GET /api/trades`): Trade → Strategy 관계에서 `@EntityGraph`로 fetch join 처리

```java
@EntityGraph(attributePaths = {"strategy"})
List<Trade> findByMemberId(Long memberId);
```

**Strategy 조회** (`GET /api/strategies`): Strategy → BuyStrategy, SellStrategy 두 컬렉션을 동시에 fetch join하면 `MultipleBagFetchException`이 발생하므로 `@BatchSize`로 IN 쿼리 일괄 처리

```java
@BatchSize(size = 100)
@OneToMany(mappedBy = "strategy")
private List<BuyStrategy> buyStrategies = new ArrayList<>();
```

### 2. QueryDSL 동적 쿼리 — 활성 전략 조회

매 체결 틱마다 거래소와 마켓 조건으로 활성 전략을 조회합니다.
`BooleanExpression`을 조합해 조건이 null이면 자동으로 해당 조건을 제외합니다.

```java
private BooleanExpression exchangeEq(Exchange exchange) {
    return exchange != null ? strategy.exchange.eq(exchange) : null;
}
```

### 3. 낙관적 락 (Optimistic Lock) — 동시 매수 충돌 방지

여러 전략이 동시에 같은 멤버의 잔고를 차감하려 할 때 `@Version`으로 충돌을 감지합니다.
`ObjectOptimisticLockingFailureException` 발생 시 해당 매수 시도만 스킵하고 나머지는 정상 처리합니다.

```java
@Version
private Long version;  // Member 엔티티
```

### 4. In-Memory 실시간 데이터 처리

캔들 데이터와 현재가를 DB 대신 `ConcurrentHashMap` 기반 메모리 저장소에서 관리합니다.
매 체결 틱마다 DB를 조회하는 대신 메모리에서 즉시 읽어 지표를 계산하고, 매매 신호 발생 시에만 DB에 기록합니다.

| 저장소 | 역할 |
|--------|------|
| `CandleStore` | 마켓별 캔들 슬라이딩 윈도우 (최대 50개) 유지 |
| `CurrentPriceStore` | 마켓별 현재가 캐싱 |
| `ActiveStrategyCache` | 매수 대기 중인 활성 전략 목록 (`isActive=true`) |
| `ActiveTradeCache` | OPEN 포지션 보유 중인 거래 목록 (`isActive=false` 상태) |

### 5. 헥사고날 아키텍처 — 의존성 역전으로 계층 분리

Application 계층이 외부 어댑터(웹, DB)를 직접 알지 못하도록 포트(인터페이스)로 추상화합니다.

- **인바운드 포트 (UseCase)**: Controller / TickProcessor가 Service 구현체 대신 `StrategyUseCase`, `TradeUseCase` 인터페이스에 의존
- **아웃바운드 포트**: Service가 JPA Repository 대신 `StrategyPort`, `TradePort` 등 인터페이스에 의존. 실제 JPA 구현은 `PersistenceAdapter`가 담당

```java
// Service는 포트(인터페이스)만 알고, 구현체(JPA)는 모름
public class StrategyService implements StrategyUseCase {
    private final StrategyPort strategyPort;   // ← 인터페이스
    private final MemberPort memberPort;       // ← 인터페이스
}
```

### 6. 단일 책임 원칙 (SRP) — WebSocket 컴포넌트 분리

초기에는 하나의 클래스가 WebSocket 연결, 재연결, 틱 처리를 모두 담당했으나 책임이 집중되어 분리했습니다.

| 클래스 | 책임 |
|--------|------|
| `UpbitWebSocketRunner` | 초기 캔들 적재 및 연결 시작 |
| `UpbitWebSocketClient` | WebSocket 연결 / 지수 백오프 재연결 |
| `TickProcessor` | 틱 수신 후 전략 평가 및 매매 실행 |
| `IndicatorCalculator` | RSI / MA / VOLUME 지표 계산 |
| `StrategyEvaluator` | 전략 조건 충족 여부 판단 |

### 7. Refresh Token 인증 — Redis 기반 3중 보안

JWT의 Stateless 특성으로 인해 서버에서 토큰을 무효화할 수 없는 문제를 Redis로 해결했습니다.

- **Access Token**: 15분 단기 유효 (탈취 피해 최소화)
- **Refresh Token**: UUID 기반, Redis에 TTL 7일로 저장
- **Access Token 블랙리스트**: 로그아웃 시 잔여 TTL만큼 Redis에 등록 → 로그아웃 후 토큰 즉시 무효화
- **Refresh Token Rotation**: 재발급 시 새 Refresh Token도 함께 발급, 기존 Refresh Token 즉시 폐기 → 탈취된 RT 재사용 차단
- **Redis 선택 이유**: TTL 기능으로 만료 토큰 자동 삭제, 인메모리 조회로 빠른 응답

```
[로그인]    → Access Token (15분) + Refresh Token (Redis, 7일)
[API 요청]  → Access Token으로 인증 + 블랙리스트 여부 확인
[토큰 만료] → POST /api/auth/refresh → Redis 검증 → 새 Access Token + 새 Refresh Token 발급
[로그아웃]  → Access Token 블랙리스트 등록 + Redis에서 Refresh Token 삭제
```

### 8. 전략 생명주기와 잔고 관리

전략은 **재사용 가능**하며, 매수 체결 시점부터 사용자 제어를 벗어나 매도까지 자동으로 완결됩니다.

```
[비활성]
   ↓ activate (buyAmount 선차감)
[활성 — 매수 대기]       isActive=true,  포지션 없음
   ↓ 매수 조건 충족 → buy() 체결
[비활성 — 매도 대기]     isActive=false, OPEN 포지션 (ActiveTradeCache에서 관리)
   ↓ 매도 조건 충족 → sell() 체결
[완료]                   isActive=false, 잔고에 수익/손실 반영
   ↓ activate 다시 클릭 → 동일 전략 재사용 가능
```

| 상태 전환 | 잔고 처리 |
|-----------|-----------|
| 비활성 → 활성 | `buyAmount` 선차감 (잔고 부족 시 예외) |
| 매수 체결 | `isActive=false` 자동 전환 (잔고 변화 없음, 포지션 평가액으로 전환) |
| 매도 체결 | 현재가 × 수량 잔고 반환 (수익/손실 반영), 캐시에서 완전 제거 |
| 활성 → 강제 비활성 (포지션 없음) | `buyAmount` 전액 환불 |
| 활성 → 강제 비활성 (포지션 있음) | 현재가로 즉시 청산 후 환불 |

---

## 📐 도메인 모델

```
Member (1) ──── (N) Strategy (1) ──── (N) Trade
                     |
                     |── (N) BuyStrategy
                     └── (N) SellStrategy
```

| 엔티티 | 설명 |
|--------|------|
| `Member` | 회원, 모의 잔고, 낙관적 락(`@Version`) |
| `Strategy` | 투자 전략, 매수/매도 조건 묶음, 활성화 여부. 재활성화로 반복 사용 가능 |
| `BuyStrategy` | 매수 조건 (지표 종류 + 기준값) |
| `SellStrategy` | 매도 조건 (지표 종류 + 기준값) |
| `Trade` | 체결 내역, 매수/매도 가격, 수익금, 수익률, 체결 시각 |

---

## 📬 API 명세

### 인증 (`/api/auth`)

| Method | URI | 설명 | 인증 |
|--------|-----|------|------|
| POST | `/api/auth/signup` | 회원가입 | 불필요 |
| POST | `/api/auth/login` | 로그인 (Access Token + Refresh Token 발급) | 불필요 |
| POST | `/api/auth/refresh` | Access Token + Refresh Token 재발급 (Rotation) | 불필요 |
| POST | `/api/auth/logout` | 로그아웃 (Access Token 블랙리스트 + Refresh Token 삭제) | 필요 |

### 전략 (`/api/strategies`)

| Method | URI | 설명 | 인증 |
|--------|-----|------|------|
| POST | `/api/strategies` | 전략 생성 | 필요 |
| GET | `/api/strategies` | 내 전략 목록 조회 | 필요 |
| POST | `/api/strategies/{id}/activate` | 전략 활성화 (잔고 차감) | 필요 |
| POST | `/api/strategies/{id}/deactivate` | 전략 비활성화 (잔고 환불) | 필요 |

### 회원 (`/api/members`)

| Method | URI | 설명 | 인증 |
|--------|-----|------|------|
| GET | `/api/members/me` | 내 정보 조회 (총 평가금액, 잔고, 주문가능금액, 전략예약금, OPEN 포지션 평가액) | 필요 |

### 거래 (`/api/trades`)

| Method | URI | 설명 | 인증 |
|--------|-----|------|------|
| GET | `/api/trades` | 내 거래 내역 조회 | 필요 |

## 📬 Contact
- GitHub: [github.com/jhklim](https://github.com/jhklim)
