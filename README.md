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
    ↓ HTTP
[ API Gateway ]
    ↓
[ Service ]
    ↑
[ Upbit WebSocket / REST API ]
```

> 각 서비스는 독립된 DB를 소유하며, 서비스 간 통신은 ID 참조 + OpenFeign(HTTP) 방식으로 처리합니다.

---

## ✨ 주요 기능

### 1. 회원 인증
- JWT 기반 로그인/회원가입
- Spring Security 필터 체인 구성

### 2. 실시간 시세 수신
- 업비트 WebSocket 연동 (체결가, 호가 실시간 수신)
- 업비트 REST API로 캔들 데이터 주기적 수집 (`@Scheduled`)
- 캔들 기반 보조지표 계산 (RSI, MA)

### 3. 전략 관리 (CRUD)
- MA 교차 전략: 단기/장기 이동평균선 교차 시 매수/매도
- RSI 전략: 설정한 과매수/과매도 기준값 기반 매매
- 사용자가 직접 파라미터 설정 (기간, 임계값 등)

### 4. 모의 자동매매 실행
- 전략 조건 충족 시 자동 매수/매도 실행
- 체결 시점 가격 스냅샷 저장

### 5. 포트폴리오 / 수익률 조회
- 보유 코인 현황 및 평균매수가 조회
- 전략별 수익률 비교
- 거래 히스토리 QueryDSL 동적 필터링

---

## ✅ 진행 현황

| 기능 | 상태      |
|------|---------|
| 프로젝트 구조 설계 (멀티모듈) | ✅ 완료    |
| DB 설계 (ERD) | ✅ 완료    |
| Member Entity 설계 | ✅ 완료    |
| Strategy / Trade Entity 설계 | ✅ 완료    |
| Upbit WebSocket 연동 | 🔄 진행 중 |
| 캔들 데이터 수집 REST API | 🔄 진행 중 |
| JWT 인증 | ✅ 예정    |
| 전략 CRUD API | ⬜ 예정    |
| 보조지표 계산 (RSI, MA) | ⬜ 예정    |
| 모의 매매 실행 엔진 | ⬜ 예정    |
| 포트폴리오 / 수익률 조회 | ⬜ 예정    |
| Swagger 문서화 | ⬜ 예정    |

---

## 📬 Contact
- GitHub: [github.com/jhklim](https://github.com/jhklim)
