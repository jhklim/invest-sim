# 📈 InvestSim — 전략 기반 암호화폐 모의 자동매매 시스템

> 실시간 시세 데이터를 기반으로 사용자가 직접 설계한 투자 전략의 수익성을 검증하는 모의 자동매매 플랫폼

![Java](https://img.shields.io/badge/Java-17-blue)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.x-green)
![MySQL](https://img.shields.io/badge/MySQL-8.x-orange)
![JWT](https://img.shields.io/badge/JWT-Auth-purple)
![WebSocket](https://img.shields.io/badge/WebSocket-Upbit-red)
![Status](https://img.shields.io/badge/Status-In_Progress-yellow)

---

## 🗂 목차
- [프로젝트 개요](#-프로젝트-개요)
- [기술 스택](#-기술-스택)
- [시스템 아키텍처](#-시스템-아키텍처)
- [핵심 설계 포인트](#-핵심-설계-포인트)
- [주요 기능](#-주요-기능)
- [진행 현황](#-진행-현황)

---

## 📌 프로젝트 개요

InvestSim은 업비트(Upbit) 거래소의 실시간 시세 데이터를 WebSocket으로 수신하고,  
사용자가 직접 설정한 보조지표 기반 전략 조건에 따라 모의 매수/매도를 자동 실행하는 백엔드 시스템입니다.

실제 자산을 사용하지 않고 전략의 수익성과 유효성을 검증하는 것이 핵심 목적입니다.

### 기획 의도
- 단순 CRUD를 넘어 **실시간 데이터 처리 + 자동화 로직**을 구현하는 경험
- **JPA 심화**(N+1 해결, QueryDSL 동적 쿼리, 낙관적 락)를 실전에서 적용
- **MSA 구조**로 서비스 경계를 명확히 분리하는 설계 

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
| Infra | Docker |
| Docs | Swagger (SpringDoc) |
| Test | JUnit 5, H2 |
| Tools | IntelliJ IDEA, DBeaver, Gradle |

---

## 🏗 시스템 아키텍처

```
[ Client ]
    ↓ HTTP REST API
[ Controller Layer ]
    ↓
[ Service Layer ]
    ↓                        ↑
[ Repository Layer ]   [ Upbit WebSocket / REST API ]
    ↓                        ↓
[ MySQL (Docker) ]     [ In-Memory Store (CandleStore, CurrentPriceStore) ]
```

---

## 💡 핵심 설계 포인트

### 1. N+1 문제 해결 — @BatchSize
전략 목록 조회 시 `BuyStrategy`, `SellStrategy` 컬렉션을 `@BatchSize`로 처리해 N+1 문제를 해결했습니다.
컬렉션 2개를 동시에 fetch join하면 발생하는 `MultipleBagFetchException`을 피하면서도 쿼리 수를 최소화했습니다.

### 2. QueryDSL 동적 쿼리
거래소(`Exchange`)와 마켓(`market`) 조건을 `BooleanExpression`으로 조합하는 동적 쿼리를 구현했습니다.
조건이 null이면 자동으로 해당 조건을 제외해 유연한 검색이 가능합니다.

```java
private BooleanExpression exchangeEq(Exchange exchange) {
    return exchange != null ? strategy.exchange.eq(exchange) : null;
}
```

### 3. 낙관적 락 (Optimistic Lock)
동시에 여러 전략이 같은 멤버의 잔고를 차감하려 할 때 `@Version`으로 충돌을 감지하고
`ObjectOptimisticLockingFailureException`을 처리해 데이터 정합성을 보장합니다.

### 4. In-Memory 실시간 데이터 처리
캔들 데이터와 현재가를 DB 대신 `ConcurrentHashMap` 기반 메모리 저장소에 유지합니다.
매매 조건 충족 시에만 DB에 기록해 불필요한 I/O를 최소화했습니다.

### 5. 단일 책임 원칙 (SRP) 적용
| 클래스 | 역할 |
|--------|------|
| `UpbitWebSocketRunner` | 실시간 데이터 수신만 담당 |
| `IndicatorCalculator` | RSI/MA 보조지표 계산만 담당 |
| `StrategyEvaluator` | 전략 조건 체크만 담당 |
| `TradeService` | 모의 매매 체결 + DB 저장만 담당 |

---

## ✨ 주요 기능

### 1. 회원 인증
- JWT 기반 로그인/회원가입
- Spring Security 필터 체인 구성
- 모의 잔고 관리 (충전/차감/환불)

### 2. 실시간 시세 수신
- 업비트 WebSocket 연동 (체결가, 호가 실시간 수신)
- 프로그램 시작 시 REST API로 초기 캔들 50개 적재
- 이후 WebSocket 체결가로 캔들 실시간 업데이트

### 3. 보조지표 계산
- RSI (지수 이동평균 방식, 업비트 기준 공식 적용)
- 거래량(VOLUME) 조건 지원
- 추후 MA 교차 전략 확장 예정

### 4. 전략 관리
- 사용자가 매수/매도 조건을 직접 설정 (RSI 기준값, 거래량 기준값 등)
- 매수/매도 조건 각각 여러 개 조합 가능 (AND 조건)
- 전략 활성화 시 투자 금액 잔고에서 선차감 (묶기)
- 전략 비활성화 시 매수 전이면 전액 환불, 매수 후면 현재가로 수익금 환불

### 5. 모의 자동매매 실행
- 전략 조건 충족 시 자동 매수 실행
- 체결 가격/수량/수익률 DB 저장
- 낙관적 락으로 동시 매수 충돌 방지

---

## ✅ 진행 현황

| 기능 | 상태 |
|------|------|
| DB 설계 (ERD) | ✅ 완료 |
| Member Entity + 잔고/낙관적 락 | ✅ 완료 |
| Strategy / BuyStrategy / SellStrategy Entity | ✅ 완료 |
| Trade Entity (수익률 기록 포함) | ✅ 완료 |
| Upbit WebSocket 연동 | ✅ 완료 |
| 초기 캔들 REST API 수집 | ✅ 완료 |
| In-Memory CandleStore / CurrentPriceStore | ✅ 완료 |
| RSI 보조지표 계산 (EMA 방식) | ✅ 완료 |
| StrategyEvaluator (전략 조건 체크) | ✅ 완료 |
| QueryDSL 동적 쿼리 (전략 조회) | ✅ 완료 |
| TradeService (모의 매수) | ✅ 완료 |
| StrategyService (활성화/비활성화) | ✅ 완료 |
| JWT 인증 | ✅ 완료 |
| 단위 테스트 / 통합 테스트 | 🔄 진행 중 |
| 전략 CRUD API (Controller) | ⬜ 예정 |
| 모의 매도 기능 | ⬜ 예정 |
| MA 교차 전략 | ⬜ 예정 |
| Swagger 문서화 | ⬜ 예정 |

---

## 📬 Contact
- GitHub: [github.com/jhklim](https://github.com/jhklim)
