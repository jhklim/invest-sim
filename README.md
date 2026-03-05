# 📈 InvestSim — 전략 기반 암호화폐 모의 자동매매 시스템

> 실시간 시세 데이터를 기반으로 사용자가 직접 설계한 투자 전략의 수익성을 검증하는 모의 자동매매 백엔드 플랫폼

![Java](https://img.shields.io/badge/Java-17-blue)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.x-green)
![MySQL](https://img.shields.io/badge/MySQL-8.x-orange)
![JWT](https://img.shields.io/badge/JWT-Auth-purple)
![WebSocket](https://img.shields.io/badge/WebSocket-Upbit-red)
![Status](https://img.shields.io/badge/Status-In_Progress-yellow)

---

## 목차
- [프로젝트 개요](#-프로젝트-개요)
- [기술 스택](#-기술-스택)
- [실시간 데이터 흐름](#-실시간-데이터-흐름)
- [핵심 설계 포인트](#-핵심-설계-포인트)
- [도메인 모델](#-도메인-모델)
- [API 명세](#-api-명세)
- [진행 현황](#-진행-현황)

---

## 📌 프로젝트 개요

InvestSim은 업비트(Upbit) 거래소의 실시간 체결 데이터를 WebSocket으로 수신하고,
사용자가 직접 설정한 보조지표 기반 전략 조건에 따라 모의 매수/매도를 자동 실행하는 백엔드 시스템입니다.

실제 자산을 사용하지 않고 전략의 수익성과 유효성을 검증하는 것이 핵심 목적입니다.

### 기획 의도
- 단순 CRUD를 넘어 **실시간 데이터 처리 + 자동화 로직**을 구현하는 경험
- **JPA 심화** (N+1 해결, QueryDSL 동적 쿼리, 낙관적 락)를 실전에서 적용
- SRP 원칙에 따라 **단일 책임을 가진 컴포넌트로 분리**하는 설계 경험

---

## 🛠 기술 스택

| 구분 | 기술 |
|------|------|
| Language | Java 17 |
| Framework | Spring Boot 3.x, Spring Data JPA, Spring Security |
| Query | QueryDSL 5.x |
| Database | MySQL 8.x (Docker), H2 (테스트) |
| Auth | JWT (jjwt) |
| Real-time | WebSocket (Upbit API) |
| Docs | Swagger (SpringDoc) |
| Test | JUnit 5, H2 In-Memory |
| Tools | IntelliJ IDEA, DBeaver, Gradle |

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
[Controller Layer]  ──>  [Service Layer]  ──>  [Repository Layer]  ──>  [MySQL]
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

### 5. 단일 책임 원칙 (SRP) — WebSocket 컴포넌트 분리

초기에는 하나의 클래스가 WebSocket 연결, 재연결, 틱 처리를 모두 담당했으나 책임이 집중되어 분리했습니다.

| 클래스 | 책임 |
|--------|------|
| `UpbitWebSocketRunner` | 초기 캔들 적재 및 연결 시작 |
| `UpbitWebSocketClient` | WebSocket 연결 / 지수 백오프 재연결 |
| `TickProcessor` | 틱 수신 후 전략 평가 및 매매 실행 |
| `IndicatorCalculator` | RSI / MA / VOLUME 지표 계산 |
| `StrategyEvaluator` | 전략 조건 충족 여부 판단 |

### 6. 전략 생명주기와 잔고 관리

전략 활성화/비활성화 시 잔고 정합성을 보장하는 규칙을 적용합니다.

| 상태 전환 | 잔고 처리 |
|-----------|-----------|
| 비활성 → 활성 | `buyAmount` 선차감 (잔고 부족 시 예외) |
| 활성 → 비활성 (포지션 없음) | `buyAmount` 전액 환불 |
| 활성 → 비활성 (포지션 있음) | 현재가 × 수량으로 청산 후 환불 |

---

## 📐 도메인 모델

```
Member (1) ──── (N) Strategy (1) ──── (1) Trade
                     |
                     |── (N) BuyStrategy
                     └── (N) SellStrategy
```

| 엔티티 | 설명 |
|--------|------|
| `Member` | 회원, 모의 잔고, 낙관적 락(`@Version`) |
| `Strategy` | 투자 전략, 매수/매도 조건 묶음, 활성화 여부 |
| `BuyStrategy` | 매수 조건 (지표 종류 + 기준값) |
| `SellStrategy` | 매도 조건 (지표 종류 + 기준값) |
| `Trade` | 체결 내역, 매수/매도 가격, 수익금, 수익률 |

---

## 📬 API 명세

### 인증 (`/api/auth`)

| Method | URI | 설명 | 인증 |
|--------|-----|------|------|
| POST | `/api/auth/signup` | 회원가입 | 불필요 |
| POST | `/api/auth/login` | 로그인 (JWT 발급) | 불필요 |

### 전략 (`/api/strategies`)

| Method | URI | 설명 | 인증 |
|--------|-----|------|------|
| POST | `/api/strategies` | 전략 생성 | 필요 |
| GET | `/api/strategies` | 내 전략 목록 조회 | 필요 |
| POST | `/api/strategies/{id}/activate` | 전략 활성화 (잔고 차감) | 필요 |
| POST | `/api/strategies/{id}/deactivate` | 전략 비활성화 (잔고 환불) | 필요 |

### 거래 (`/api/trades`)

| Method | URI | 설명 | 인증 |
|--------|-----|------|------|
| GET | `/api/trades` | 내 거래 내역 조회 | 필요 |

> Swagger UI: `http://localhost:8080/swagger-ui/index.html`

---

## ✅ 진행 현황

| 기능 | 상태 |
|------|------|
| DB 설계 (ERD) | ✅ 완료 |
| Member 엔티티 + 잔고 / 낙관적 락 | ✅ 완료 |
| Strategy / BuyStrategy / SellStrategy 엔티티 | ✅ 완료 |
| Trade 엔티티 (수익률 기록 포함) | ✅ 완료 |
| Upbit WebSocket 연동 (지수 백오프 재연결) | ✅ 완료 |
| 초기 캔들 REST API 수집 | ✅ 완료 |
| In-Memory CandleStore / CurrentPriceStore | ✅ 완료 |
| RSI 보조지표 계산 (EMA 방식) | ✅ 완료 |
| MA 단순 이동평균 계산 | ✅ 완료 |
| StrategyEvaluator (전략 조건 체크) | ✅ 완료 |
| QueryDSL 동적 쿼리 (활성 전략 조회) | ✅ 완료 |
| TradeService (모의 매수 / 매도) | ✅ 완료 |
| StrategyService (활성화 / 비활성화) | ✅ 완료 |
| JWT 인증 (Spring Security) | ✅ 완료 |
| 전략 CRUD API | ✅ 완료 |
| 거래 내역 조회 API | ✅ 완료 |
| WebSocket SRP 리팩토링 (3개 클래스 분리) | ✅ 완료 |
| Bean Validation (@Valid) | ✅ 완료 |
| Swagger 문서화 | ✅ 완료 |
| 단위 / 통합 테스트 | 🔄 진행 중 |
| MA 교차 전략 (골든크로스 / 데드크로스) | ⬜ 예정 |
| 다중 마켓 지원 (현재 KRW-BTC 고정) | ⬜ 예정 |

---

## 📬 Contact
- GitHub: [github.com/jhklim](https://github.com/jhklim)
