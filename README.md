# 🎫 프로모션을 진행하는 쇼핑몰 서비스

## 📌 프로젝트 개요
### 소개
- 대규모 프로모션 환경을 가정하고 시스템에서 발생하는 트래픽 부하와 동시성 문제 해결에 중점을 둔 프로젝트입니다.

## 🛠 기술 스택
- **Backend**: Java 17, Spring Boot 3.4.1, JPA
- **Database**: MySQL, Redis
- **Message Queue**: Apache Kafka
- **Infrastructure**: AWS (EC2, RDS), Docker
- **Monitoring & Logging**: Elasticsearch, Logstash, Kibana (ELK Stack)
- **Testing**: K6

## 🎯 프로젝트 목표
### 1. 대용량 트래픽 처리 아키텍처 구현
- MAU 5,000만, DAU 1,000만 사용자 대상 서비스 설계
- Redis 기반 분산 락과 Kafka 이벤트 드리븐 아키텍처 적용

### 2. MSA 기반 서비스 아키텍처
- 하나의 서비스 장애가 다른 서비스들로 연쇄적으로 전파되어 전체 시스템이 마비되는 상황을 방지
- 한개의 모듈만 확장 가능한 서비스

### 3. 장애 격리 및 고가용성 시스템
- Circuit Breaker 패턴을 통한 서비스 간 장애 전파 방지
- Transaction Outbox 패턴으로 분산 환경 데이터 일관성 보장
- Redis Sentinel을 통한 자동 페일오버 및 무중단 서비스

## 📊 ERD
![Image](https://github.com/user-attachments/assets/97ad6acb-36a3-4f8d-89db-8a2fea2ffbc1)

## 📋 API 문서

### 쿠폰 서비스 (Coupon Service)

**쿠폰 API**
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v3/coupons` | 쿠폰 발급 요청 |
| POST | `/api/v3/coupons/{couponId}/use` | 쿠폰 사용 |
| POST | `/api/v3/coupons/{couponId}/cancel` | 쿠폰 사용 취소 |

**쿠폰 정책 API**
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v3/coupon-policies` | 쿠폰 정책 생성 |
| GET | `/api/v3/coupon-policies/{id}` | 쿠폰 정책 상세 조회 |
| GET | `/api/v3/coupon-policies` | 모든 쿠폰 정책 목록 조회 |

### 주문 서비스 (Order Service)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v3/orders` | 주문 생성 |
| POST | `/api/v3/orders/cancel` | 주문 취소 |
| POST | `/api/v3/orders/cart` | 장바구니 주문 |

### 포인트 서비스 (Point Service)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v2/points/earn` | 포인트 적립 |
| POST | `/api/v2/points/use` | 포인트 사용 |
| POST | `/api/v2/points/{pointId}/cancel` | 포인트 사용 취소 |
| GET | `/api/v2/points/users/{userId}/balance` | 사용자 포인트 잔액 조회 |
| GET | `/api/v2/points/users/{userId}/history` | 사용자 포인트 사용 내역 조회 |

### 사용자 서비스 (User Service)

**인증 API**
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/users/login` | 로그인 |
| POST | `/api/v1/users/validate-token` | 토큰 유효성 검증 |
| POST | `/api/v1/users/refresh-token` | 토큰 갱신 |

**사용자 관리 API**
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/users/signup` | 회원가입 |
| GET | `/api/v1/users/me` | 사용자 프로필 조회 |
| PUT | `/api/v1/users/me` | 사용자 프로필 수정 |
| POST | `/api/v1/users/me/password` | 비밀번호 변경 |
| GET | `/api/v1/users/me/login-history` | 로그인 이력 조회 |

**장바구니 API**
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/carts/{id}` | 장바구니 조회 |
| POST | `/api/v1/carts` | 장바구니에 상품 추가 |
| PUT | `/api/v1/carts/product/{originalProductId}/option/{originalOptionId}` | 장바구니 옵션 수정 |
| DELETE | `/api/v1/carts` | 장바구니 상품 삭제 |
| DELETE | `/api/v1/carts/{userId}/all` | 장바구니 전체 비우기 |


## 🔍 중점적으로 고민했던 기술 요소와 해결

### 1. 쿠폰 발급 시스템 3단계 아키텍처 개선을 통한 대용량 트래픽 처리

**[도입 배경]**
> 시스템 설계 목표: MAU 5,000만, DAU 1,000만 사용자를 대상으로 한 쿠폰 서비스 구축 가정 <br/>
>> QPD(Query Per Day) = DAU × 발행 액션 수 × 세션당 쿼리 수 = 1,000만 × 2 × 3 = 6,000만건 <br/>
>> 읽기 QPS = 4,000만건 ÷ 86,400초 ≈ 463 QPS (쿠폰 정책 조회 + 발행 수량 체크) <br/>
>> 쓰기 TPS = 2,000만건 ÷ 86,400초 ≈ 231 TPS (쿠폰 저장) <br/>
>> Peak 시간대에는 평상시의 4~5배인 초당 1,000+ TPS의 쿠폰 발행 API 처리 가정 <br/>

초기 DB 기반 쿠폰 발급 시스템에서는 대규모 프로모션 이벤트 시 목표 성능을 달성할 수 없었습니다. 특히 선착순 쿠폰 이벤트에서 동시성 문제로 인한 중복 발급과 시스템 불안정 문제가 지속적으로 발생하여 근본적인 아키텍처 개선이 필요했습니다.

**[사용 이유]**
DB 기반에서 Redis 기반으로 1차 개선하여 메모리 기반 빠른 처리와 분산 락을 통한 동시성 제어가 가능합니다. <br/>
Redis에서 Kafka 기반으로 2차 개선하여 쿠폰 발급과 DB 저장을 분리해 비동기 처리로 응답 속도를 극대화할 수 있습니다. <br/>
Peak 시간대 1,000+ TPS 요구사항을 만족하기 위해 이벤트 드리븐 아키텍처로 시스템 확장성과 장애 내성을 확보할 수 있습니다.

**[성과]**
쿠폰 발급 성능 개선 과정 (쿠폰 정책 10개, 각 정책당 1만 건)

| 개선 단계 | 구현 방식 | 최대 TPS | 개선율 | 주요 개선 사항 |
|----------|-----------|----------|-------|----------------|
| 기준 (V1) | JPA + DB Lock + COUNT Query | 95 | - | 비관적 락으로 인한 순차 처리, COUNT 쿼리 병목 |
| 1차 개선 (V2) | Redis + Redisson Lock + Atomic Operations | 580 | 460% | Redis 원자적 연산으로 DB 부하 감소, 락 경합 최소화 |
| 2차 개선 (V3) | 비동기 메시징 + Kafka + Event-Driven | 1,250 | 126% | 비동기 처리로 응답 시간 단축, 큐를 통한 부하 분산 |

DAU 1,000만 쿠폰 발급 대비 처리량을 Peak 시간대 1,000+ TPS 요구사항을 달성했습니다.



### 2. 실시간 적립금 조회 시스템 최적화 및 대용량 배치 처리 성능 개선

**[도입 배경]**
적립금 조회 시마다 데이터베이스에 직접 접근하여 응답 시간이 200ms나 소요되어 사용자 경험이 저하되었습니다. 또한 일별 적립금 데이터 동기화 배치 작업이 200시간이나 소요되어 실시간 서비스 운영에 큰 부담이 되었습니다.

**[사용 이유]**
Redis 캐싱 시스템을 도입하여 자주 조회되는 적립금 데이터를 메모리에 저장해 빠른 응답 속도를 확보할 수 있습니다. <br/>
JPA 기반에서 JDBC 기반으로, 단일 스레드에서 멀티 스레드 병렬 처리로 단계적 개선하여 배치 성능을 최적화할 수 있습니다. <br/>
Jenkins를 통한 배치 작업 자동화로 운영 효율성을 높일 수 있습니다.

**[성과]**
Redis 캐싱 도입으로 적립금 조회 응답 속도를 200ms → 5ms로 97.5% 향상시켰습니다. <br/>
배치 처리 성능을 3단계에 걸쳐 개선하여 총 97.7% 성능 향상(200시간 → 4.58시간)을 달성했습니다.

배치 처리 성능 개선 과정 (PointBalance 500만건, Point 트랜잭션 3000만건)

| 개선 단계 | 구현 방식 | 처리 시간 | 개선율 | 문제점 및 개선 사항 |
|----------|-----------|----------|-------|-------------------|
| 기준 | JpaPagingItemReader | ~200시간 | - | LIMIT-OFFSET 방식의 성능 저하, 대용량 데이터 처리 시 비효율적 |
| 1차 개선 | JdbcCursorItemReader | 53.3시간 | 73.4% | 커서 기반 조회로 메모리 효율성 증가, 페이징 오버헤드 제거 |
| 2차 개선 | JdbcCursorItemReader + JdbcBatchItemWriter | 11.67시간 | 78.1% | 벌크 INSERT 방식으로 Writer 성능 최적화, 네트워크 트래픽 감소 |
| 3차 개선 | SynchronizedItemReader + JdbcBatchItemWriter + ThreadPoolTaskExecutor | 4.58시간 | 60.8% | 멀티스레드 병렬 처리로 CPU 자원 효율적 활용, 처리량 증가 |



### 3. MSA 아키텍처 설계를 통한 확장 가능한 시스템 구축

**[도입 배경]**
초기 모놀리식 아키텍처에서는 특정 기능에 장애가 발생하면 전체 시스템이 영향을 받았습니다. 또한 사용자 증가에 따라 시스템 확장이 필요할 때 전체 애플리케이션을 스케일링해야 하는 비효율성이 발생했습니다. 서비스별로 독립적인 개발과 배포가 어려워 개발 생산성이 저하되는 문제를 해결하고자 MSA 아키텍처를 도입했습니다.

**[사용 이유]**
마이크로서비스 아키텍처로 각 서비스의 독립성을 확보하여 장애 격리와 개별 확장이 가능합니다. <br/>
Spring Cloud Netflix Eureka를 활용한 서비스 디스커버리로 동적 스케일링 환경에서 서비스 간 통신을 자동화할 수 있습니다. <br/>
각 서비스별 헬스체크를 통해 장애를 조기 감지하고 자동 복구 메커니즘을 구현할 수 있습니다.

**[성과]**
마이크로서비스 아키텍처 기반 개별 모듈 개발로 서비스별 독립적인 개발과 배포 환경을 구축했습니다. <br/>
서비스 디스커버리를 통한 서비스 등록 및 발견 자동화로 동적 스케일링이 가능한 시스템을 구현했습니다. <br/>
장애 감지 및 자동 복구 메커니즘으로 시스템 안정성과 가용성을 크게 향상시켰습니다.

### 4. 시스템 안정성 확보를 위한 장애 격리 및 고가용성 아키텍처 구현

**[도입 배경]**
MSA 환경에서 하나의 서비스 장애가 다른 서비스들로 연쇄적으로 전파되어 전체 시스템이 마비되는 상황이 발생했습니다. 또한 Redis 단일 마스터 구조에서 장애 시 전체 캐시 서비스가 중단되어 사용자 서비스에 심각한 영향을 미쳤습니다. 이러한 시스템 불안정 문제를 해결하고자 장애 격리 및 고가용성 아키텍처를 구현했습니다.

**[사용 이유]**
CircuitBreaker 패턴을 구현하여 서비스 간 장애를 격리하고 연쇄 장애를 방지할 수 있습니다. <br/>
Transaction Outbox 패턴을 통해 분산 환경에서 이벤트 발행의 신뢰성을 확보하고 데이터 일관성을 보장할 수 있습니다. <br/>
Redis Sentinel을 구현하여 마스터 장애 시 자동 페일오버를 통한 서비스 연속성을 확보할 수 있습니다. <br/>
비동기 처리 아키텍처로 서비스 간 결합도를 낮추고 시스템 확장성을 향상시킬 수 있습니다.

**[성과]**
Resilience4j 기반 CircuitBreaker 구현으로 서비스 장애 시 대체 응답을 제공하여 사용자 경험을 개선했습니다. <br/>
Transaction Outbox 패턴으로 주문-결제-포인트-쿠폰 간 데이터 일관성을 보장하고 서비스 간 결합도를 감소시켰습니다. <br/>
Redis Sentinel 구현으로 시스템 다운타임 제로를 달성하고 대규모 프로모션 이벤트 중에도 안정적인 서비스를 제공했습니다.

### 5. ELK 스택 기반 통합 모니터링 및 로깅 시스템 구축

**[도입 배경]**
MSA 환경에서 분산된 서비스들의 로그가 각각 다른 서버에 흩어져 있고 서버 재시작이나 장애 시 로그가 유실되어 영구 저장이 불가능했습니다. 그리고 특정 시점의 로그를 찾기 위해 여러 서버를 일일이 확인해야 하는 비효율적인 상황이 지속되었습니다. 이런 문제를 해결하고자 통합 모니터링 및 로깅 시스템을 구축했습니다.

**[사용 이유]**
ELK 스택(Elasticsearch, Logstash, Kibana)을 활용하여 분산된 로그를 중앙 집중화하고 실시간 검색 및 분석이 가능합니다. <br/>
Trace ID 기반 로깅을 구현하여 분산 시스템에서 요청의 전체 흐름을 추적할 수 있습니다.

**[성과]**
Trace ID 기반 분산 시스템 추적으로 MSA 환경에서 서비스 간 요청 흐름을 한 번에 파악할 수 있어 장애 원인 분석 과정을 체계화했습니다. 






