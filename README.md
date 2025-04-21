# 🎫 프로모션을 진행하는 쇼핑몰 서비스

## 📌 프로젝트 개요
### 소개
- 대규모 프로모션 환경을 가정하고 시스템에서 발생하는 트래픽 부하와 동시성 문제 해결에 중점을 둔 프로젝트입니다.

### 핵심 기능

## 🛠 기술 스택
- **Backend**: Java 17, Spring Boot 3.4.1, JPA
- **Database**: MySQL, Redis
- **Infrastructure**: AWS (EC2, RDS, ELB, CloudWatch)
- **Testing**: K6

## 📊 ERD

## 🔍 시스템 아키텍처

## 📈 성능 테스트 결과

### 테스트 환경
| 구분 | 인스턴스 타입 | CPU | 메모리 |
|------|--------------|------|--------|
| K6 서버 | t3a.small | 2 vCPU | 2 GiB |
| EC2 서버 | t3a.small | 2 vCPU | 2 GiB |

| 테이블 | 데이터 건수 |
|--------|------------|
| 유저 | 1,000,000 건 |
| 콘서트 | 2,000,000 건 |
| 콘서트 스케줄 | 3,000,000 건 |
| 좌석 | 150,000,000 건 |
| 예약 | 2,000,000 건 |

테스트 기간: 5분<br/>
부하 패턴: 0명에서 시작하여 5분 동안 점진적으로 5,000명까지 증가


### 대기열 없는 기본 구현
- TPS: 766
- 응답시간: 4초
- EC2 CPU: 99.9%
- RDS CPU: 19.9%
<details>
  <summary>성능 테스트 이미지, CloudWatch 이미지 보기</summary>
  <img src="https://github.com/user-attachments/assets/1ca8fad6-f28b-4f36-8ec1-bae0605d387c"/>
  <img src="https://github.com/user-attachments/assets/fb21f80c-a584-4402-9d6f-e409f45b57a1"/>
</details>

### MySQL기반 대기열 적용 후

### Redis기반 대기열 적용 후 (서버 이중화 포함)

## 🎯 대기열 처리량 분석 및 설계

### 1. 대기열 미적용 시 시스템 성능 분석
| 지표 | 수치 |
|------|------|
| HTTP Request Rate | 1,110 req/s |
| Request Duration | 4초 |
| 최대 동시 접속자 | 5,000명 |
| EC2 CPU | 99.9% |
| RDS CPU | 32.7% |
| EC2 Memory | 52% |

### 2. 대기열 처리량 산정


#### 사용자별 API 호출
```

```

#### 실제 동시 처리 가능 사용자 산출

### 3. 최종 대기열 배치 설계

## 🔒 락 전략 선택 이유
### 1. 좌석 예약 - 낙관적 락(Optimistic Lock) 선택

### 2. 포인트 충전 - 비관적 락(Pessimistic Lock) 선택

### 3. 성능 최적화

## 🎯 향후 개선 사항
1. ElastiCache 도입 검토

