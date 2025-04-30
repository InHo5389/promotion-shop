# Redis 마스터-레플리카 구성 및 Sentinel 시스템 구축 보고서

## 1. 문제 상황

AWS EC2 환경에서 Redis를 운영하는 중, 다음과 같은 문제점에 직면했습니다:

1. 단일 Redis 서버를 사용하는 경우, 해당 서버에 장애가 발생하면 전체 서비스가 중단됩니다.
2. **확장성 문제**: 읽기 트래픽이 많아질 경우 단일 Redis 서버로는 처리가 어렵습니다.


## 2. 해결 방안: Redis 마스터-레플리카 + Sentinel 아키텍처

이러한 문제를 해결하기 위해 3대의 EC2 인스턴스를 사용하여 Redis 마스터-레플리카 구성 및 Sentinel 기반의 고가용성 시스템을 구축했습니다.

### 인프라 구성
- **EC2 서버 1**  : Redis 마스터 + Sentinel
- **EC2 서버 2**  : Redis 레플리카 + Sentinel
- **EC2 서버 3** : Sentinel만 실행

### 아키텍처 이점
1. **고가용성**: 마스터 노드에 장애가 발생해도 Sentinel이 자동으로 페일오버를 실행하여 레플리카를 새 마스터로 승격시킵니다.
2. **읽기 확장성**: 레플리카 노드에서 읽기 작업을 분산하여 처리할 수 있습니다.
3. **데이터 지속성**: 여러 노드에 데이터가 복제되어 단일 노드 장애 시에도 데이터 손실을 방지합니다.

## 3. 구현 과정

### 3.1. EC2 서버 1 (마스터) 구성

1. Docker 설치 및 Redis 마스터 설정:
```bash
# Docker 설치
sudo yum update -y
sudo yum install -y docker
sudo systemctl start docker
sudo systemctl enable docker

# Redis 구성 파일 생성
mkdir -p ~/redis-config
cat > ~/redis-config/redis.conf << EOF
bind 0.0.0.0
protected-mode no
port 6379
maxmemory 500mb
maxmemory-policy allkeys-lru
EOF

# Redis 마스터 실행
docker run --name redis-master -v ~/redis-config/redis.conf:/etc/redis/redis.conf -p 6379:6379 -d redis redis-server /etc/redis/redis.conf
```

2. Sentinel 설정 및 실행:
```bash
# Sentinel 설정 파일 생성
mkdir -p /etc/redis
cat > /etc/redis/sentinel.conf << EOF
port 26379
sentinel monitor mymaster 172.31.9.58 6379 2
sentinel down-after-milliseconds mymaster 5000
sentinel failover-timeout mymaster 60000
sentinel parallel-syncs mymaster 1
EOF

# 파일 권한 수정
chmod 777 /etc/redis/sentinel.conf

# Sentinel 실행
docker run --name redis-sentinel \
--network=host \
-d redis \
redis-sentinel --port 26379 --sentinel monitor mymaster 172.31.9.58 6379 2 --sentinel down-after-milliseconds mymaster 5000 --sentinel failover-timeout mymaster 60000 --sentinel parallel-syncs mymaster 1
```

### 3.2. EC2 서버 2 (레플리카) 구성

1. Docker 설치 및 Redis 레플리카 설정:
```bash
# Docker 설치 (EC1과 동일)
sudo yum update -y
sudo yum install -y docker
sudo systemctl start docker
sudo systemctl enable docker

# Redis 레플리카 실행
docker run --name redis-replica -p 6379:6379 -d redis redis-server --replicaof 172.31.9.58 6379
```

2. Sentinel 설정 및 실행 (EC1과 동일한 방식):
```bash
# Sentinel 설정 파일 생성
mkdir -p /etc/redis
cat > /etc/redis/sentinel.conf << EOF
port 26379
sentinel monitor mymaster 172.31.9.58 6379 2
sentinel down-after-milliseconds mymaster 5000
sentinel failover-timeout mymaster 60000
sentinel parallel-syncs mymaster 1
EOF

# Sentinel 실행
docker run --name redis-sentinel \
--network=host \
-d redis \
redis-sentinel --port 26379 --sentinel monitor mymaster 172.31.9.58 6379 2 --sentinel down-after-milliseconds mymaster 5000 --sentinel failover-timeout mymaster 60000 --sentinel parallel-syncs mymaster 1
```

### 3.3. EC2 서버 3 (Sentinel 전용) 구성

```bash
# Docker 설치 (다른 서버와 동일)
sudo yum update -y
sudo yum install -y docker
sudo systemctl start docker
sudo systemctl enable docker

# Sentinel 설정 및 실행
mkdir -p /etc/redis
cat > /etc/redis/sentinel.conf << EOF
port 26379
sentinel monitor mymaster 172.31.9.58 6379 2
sentinel down-after-milliseconds mymaster 5000
sentinel failover-timeout mymaster 60000
sentinel parallel-syncs mymaster 1
EOF

# Sentinel 실행
docker run --name redis-sentinel \
--network=host \
-d redis \
redis-sentinel --port 26379 --sentinel monitor mymaster 172.31.9.58 6379 2 --sentinel down-after-milliseconds mymaster 5000 --sentinel failover-timeout mymaster 60000 --sentinel parallel-syncs mymaster 1
```

## 4. 테스트 확인 및 결과

### 4.1. 페일오버 테스트
마스터 Redis 서버를 임시로 중지하여 Sentinel이 페일오버를 트리거하는지 확인했습니다:

```bash
# 마스터 서버에서 실행
docker stop redis-master
```

Sentinel 로그에서 페일오버가 성공적으로 이루어지는 것을 확인했습니다:
```
+sdown master mymaster 172.31.9.58 6379
+odown master mymaster 172.31.9.58 6379 #quorum 2/2
+new-epoch 1
+try-failover master mymaster 172.31.9.58 6379
+vote-for-leader 711672caf68e551f6c30fbc4fcd0930fa43aae16 1
+elected-leader master mymaster 172.31.9.58 6379
+failover-state-select-slave master mymaster 172.31.9.58 6379
+selected-slave slave 172.31.X.2 6379
+failover-state-send-slaveof-noone slave 172.31.X.2 6379
+failover-state-wait-promotion slave 172.31.X.2 6379
+promoted-slave slave 172.31.X.2 6379
+failover-state-reconf-slaves master mymaster 172.31.9.58 6379
+slave-reconf-done slave 172.31.X.2 6379
+failover-end master mymaster 172.31.9.58 6379
```

## 5. 결론 및 개선 사항

### 5.1. 성과
- Redis 마스터-레플리카 구성 및 Sentinel을 통한 고가용성 시스템을 성공적으로 구축했습니다.
- 페일오버 테스트를 통해 마스터 서버 장애 시 자동으로 레플리카가 마스터로 승격되는 것을 확인했습니다.
- 데이터 지속성 및 읽기 확장성을 확보했습니다.

### 5.2. 향후 개선 사항
1. **Redis Cluster 구성 고려**: 데이터 세트가 더 커지면 Redis Cluster로 전환하여 데이터 분산 및 확장성 향상
2. **모니터링 시스템 구축**: Prometheus + Grafana를 활용한 Redis 메트릭 모니터링 시스템 구축
3. **백업 전략 구현**: RDB/AOF 지속성 설정 및 주기적인 백업 전략 추가
4. **보안 강화**: Redis 인증 설정 및 네트워크 보안 강화

## 6. 참고 사항

- Redis 버전: 7.2.x (Docker 공식 이미지 최신 버전)
- Amazon Linux 2 기반 EC2 인스턴스 사용
- Redis 메모리 설정: 500MB
- Sentinel 쿼럼 설정: 2 (3대 중 2대 이상의 Sentinel이 장애를 감지해야 페일오버 발생)

이 구성을 통해 고가용성, 확장성, 성능이 모두 향상된 Redis 시스템을 구축할 수 있었습니다.
