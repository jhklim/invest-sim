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

## 🚀 라이브 데모

| 서비스 | URL |
|--------|-----|
| 프론트엔드 | https://d2xct30b72j3dd.cloudfront.net |

> **테스트 계정**: `test@gmail.com` / `test1357`

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

## ☁️ 배포 구조

```
사용자
 │
 ├──> CloudFront ──> S3 (React 빌드 파일)
 │
 └──> Nginx (EC2 t3.micro, Ubuntu)
          │  HTTPS / Let's Encrypt
          ▼
     Spring Boot (Docker)
          │
          ├─ MySQL 8 (Docker)
          └─ Redis 7 (Docker)
```

| 구성 요소 | 역할 |
|-----------|------|
| AWS EC2 (t3.micro) | Spring Boot 애플리케이션 서버 |
| Docker Compose | MySQL, Redis, Spring Boot 컨테이너 통합 관리 |
| Nginx | 리버스 프록시, HTTPS 처리 |
| Let's Encrypt | 무료 SSL 인증서 자동 발급/갱신 |
| AWS S3 | React 정적 빌드 파일 호스팅 |
| AWS CloudFront | CDN, HTTPS 적용 |
| DuckDNS | 무료 도메인 (`investsim.duckdns.org`) |

---

## 🏗 아키텍처

**헥사고날 아키텍처 (Ports & Adapters)** 를 적용해 Application 계층이 외부(웹, DB, WebSocket)에 의존하지 않도록 설계했습니다.

```
[인바운드 어댑터]                [애플리케이션]                   [아웃바운드 어댑터]

StrategyController  ──>  StrategyUseCase (in port)
TradeController     ──>  TradeUseCase (in port)         StrategyPort (out port)  ──>  StrategyPersistenceAdapter
MemberController    ──>  MemberUseCase (in port)   ──>  TradePort (out port)     ──>  TradePersistenceAdapter
TickProcessor       ──>       ↓                         MemberPort (out port)    ──>  MemberPersistenceAdapter
                         StrategyService                CurrentPricePort         ──>  CurrentPriceStore
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

**문제**: 연관 엔티티를 지연 로딩하면 Trade/Strategy 목록 조회 시 레코드 수만큼 추가 쿼리 발생

**Trade 조회** (`GET /api/trades`): Trade → Strategy 관계를 `@EntityGraph`로 fetch join 처리 → 쿼리 1회로 해결

```java
@EntityGraph(attributePaths = {"strategy"})
List<Trade> findByMemberId(Long memberId);
```

**Strategy 조회** (`GET /api/strategies`): BuyStrategy, SellStrategy 두 컬렉션을 동시에 fetch join하면 Hibernate가 `MultipleBagFetchException`을 던짐 → `@BatchSize`로 IN 쿼리 일괄 처리

```java
@BatchSize(size = 100)
@OneToMany(mappedBy = "strategy")
private List<BuyStrategy> buyStrategies = new ArrayList<>();
```

### 2. QueryDSL 동적 쿼리 — 활성 전략 조회

**문제**: 매 체결 틱마다 거래소·마켓 조건으로 활성 전략을 조회하는데, 조건이 null인 경우 정적 JPQL로는 처리가 번거롭고 조건 누락 위험

**해결**: `BooleanExpression`을 조합해 null 조건은 자동 제외. 조건이 늘어나도 메서드 추가만으로 확장 가능

```java
private BooleanExpression exchangeEq(Exchange exchange) {
    return exchange != null ? strategy.exchange.eq(exchange) : null;
}
```

### 3. 낙관적 락 (Optimistic Lock) — 동시 매수 충돌 방지

**문제**: 여러 전략이 동시에 같은 멤버의 잔고를 차감하면 Race Condition으로 잔고가 마이너스가 될 수 있음

**해결**: `Member` 엔티티에 `@Version` 적용 → 동시에 두 트랜잭션이 잔고를 수정하면 나중에 커밋되는 쪽에서 `ObjectOptimisticLockingFailureException` 발생 → 해당 매수만 스킵, 나머지는 정상 처리

```java
@Version
private Long version;  // Member 엔티티
```

### 4. In-Memory 실시간 데이터 처리

**문제**: 초당 수십 건의 체결 틱이 들어올 때 매번 DB에서 캔들·현재가를 조회하면 응답 지연 및 DB 부하 발생

**해결**: `ConcurrentHashMap` 기반 메모리 저장소에서 즉시 읽어 지표를 계산하고, 매매 신호 발생 시에만 DB에 기록

| 저장소 | 역할 |
|--------|------|
| `CandleStore` | 마켓별 캔들 슬라이딩 윈도우 (최대 50개) 유지 |
| `CurrentPriceStore` | 마켓별 현재가 캐싱 |
| `ActiveStrategyCache` | 매수 대기 중인 활성 전략 목록 (`isActive=true`) |
| `ActiveTradeCache` | OPEN 포지션 보유 중인 거래 목록 (`isActive=false` 상태) |

### 5. 헥사고날 아키텍처 — 의존성 역전으로 계층 분리

**문제**: 계층 간 직접 의존 시 DB 변경, 프레임워크 교체가 전 계층에 영향. 테스트 시 외부 인프라 없이 Application 로직만 검증하기 어려움

**해결**: Application 계층은 포트(인터페이스)만 알고, 실제 구현(JPA, WebSocket)은 어댑터가 담당

- **인바운드 포트 (UseCase)**: Controller / TickProcessor → `StrategyUseCase`, `TradeUseCase` 인터페이스에 의존
- **아웃바운드 포트**: Service → `StrategyPort`, `TradePort` 인터페이스에 의존. JPA 구현은 `PersistenceAdapter`가 담당

```java
// Service는 포트(인터페이스)만 알고, 구현체(JPA)는 모름
public class StrategyService implements StrategyUseCase {
    private final StrategyPort strategyPort;   // ← 인터페이스
    private final MemberPort memberPort;       // ← 인터페이스
}
```

### 6. 단일 책임 원칙 (SRP) — WebSocket 컴포넌트 분리

**문제**: 초기 `UpbitWebSocketRunner` 하나가 WebSocket 연결, 재연결, 틱 처리, 전략 평가를 모두 담당 → 클래스 비대화, 변경 시 사이드 이펙트 위험

**해결**: 책임 단위로 분리

| 클래스 | 책임 |
|--------|------|
| `UpbitWebSocketRunner` | 초기 캔들 적재 및 연결 시작 |
| `UpbitWebSocketClient` | WebSocket 연결 / 지수 백오프 재연결 |
| `TickProcessor` | 틱 수신 후 전략 평가 및 매매 실행 |
| `IndicatorCalculator` | RSI / MA / VOLUME 지표 계산 |
| `StrategyEvaluator` | 전략 조건 충족 여부 판단 |

### 7. Refresh Token 인증 — Redis 기반 3중 보안

**문제**: JWT는 Stateless라 서버에서 토큰을 무효화할 수 없음 → 로그아웃 후에도 만료 전 Access Token으로 API 호출 가능, Refresh Token 탈취 시 재사용 가능

**해결**: Redis를 활용한 3중 보안 구조

- **Access Token 블랙리스트**: 로그아웃 시 잔여 TTL만큼 Redis에 등록 → 즉시 무효화
- **Refresh Token Rotation**: 재발급 시 새 RT 발급 + 기존 RT 즉시 폐기 → 탈취된 RT 재사용 차단
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
| POST | `/api/auth/signup` | 회원가입 | ❌ |
| POST | `/api/auth/login` | 로그인 | ❌ |
| POST | `/api/auth/refresh` | Access/Refresh Token 재발급 (Rotation) | ❌ |
| POST | `/api/auth/logout` | 로그아웃 | ✅ |

### 전략 (`/api/strategies`)

| Method | URI | 설명 | 인증 |
|--------|-----|------|------|
| POST | `/api/strategies` | 전략 생성 | ✅ |
| GET | `/api/strategies` | 내 전략 목록 조회 | ✅ |
| POST | `/api/strategies/{id}/activate` | 전략 활성화 (잔고 차감) | ✅ |
| POST | `/api/strategies/{id}/deactivate` | 전략 비활성화 (잔고 환불) | ✅ |

### 회원 (`/api/members`)

| Method | URI | 설명 | 인증 |
|--------|-----|------|------|
| GET | `/api/members/me` | 내 정보 조회 (잔고, 평가금액, 예약금, OPEN 포지션) | ✅ |

### 거래 (`/api/trades`)

| Method | URI | 설명 | 인증 |
|--------|-----|------|------|
| GET | `/api/trades` | 내 거래 내역 조회 | ✅ |

---

## 📬 Contact
- GitHub: [github.com/jhklim](https://github.com/jhklim)
