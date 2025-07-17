# Kafka Streams PostgreSQL Materialized View Example

이 프로젝트는 Kafka Streams를 사용하여 PostgreSQL 기반의 고급 Materialized View를 구현하는 예제입니다.

## 아키텍처

```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│             │    │             │    │             │
│ User Service├────► Kafka Topic ├────► Post Service│
│             │    │             │    │             │
└─────────────┘    └─────────────┘    └─────┬───────┘
                                              │
                                              ▼
                                   ┌─────────────────┐
                                   │   PostgreSQL    │
                                   │ Materialized    │
                                   │     View        │
                                   └─────────────────┘
```

## 주요 기능

### 1. Kafka Streams 기반 실시간 처리
- User 이벤트 스트림 처리
- 실시간 Materialized View 업데이트
- Exactly-once 처리 보장

### 2. PostgreSQL Materialized View
- 사용자 정보 실시간 동기화
- 부서별 통계 자동 업데이트
- 인덱스 최적화

### 3. 고급 기능
- 이벤트 소싱 패턴
- CQRS 구현
- 장애 복구 및 재처리

## 시작하기

### 1. 환경 실행 멀티스테이지 빌드 캐시 무시하고 재빌드
```bash
# 모든 서비스 시작
docker-compose up -d
# 안되면 Docker 빌드 캐시 무시하고 재빌드
docker-compose build --no-cache

# 로그 확인
docker-compose logs -f post-service-kafka
```

### 2. 사용자 생성
```bash
# 사용자 생성
curl -X POST http://localhost:8081/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "name": "홍길동",
    "email": "hong@example.com",
    "department": "개발팀",
    "status": "ACTIVE"
  }'

  curl -X POST http://localhost:8081/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "name": "장웅",
    "email": "changwng@example.com",
    "department": "개발팀",
    "status": "ACTIVE"
  }'


  curl -X POST http://localhost:8081/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "name": "new_장웅",
    "email": "new_changwng@example.com",
    "department": "개발팀",
    "status": "ACTIVE"
  }'
```

### 3. 포스트 생성
```bash
# 포스트 생성 (사용자 정보 자동 enrichment)
curl -X POST http://localhost:8082/api/posts \
  -H "Content-Type: application/json" \
  -d '{
    "title": "안녕하세요",
    "content": "첫 번째 포스트입니다.",
    "authorId": 6
  }'
```

### 4. Materialized View 확인
```bash
# 모든 사용자 조회
curl http://localhost:8082/api/posts/users

# 부서별 포스트 조회
curl http://localhost:8082/api/posts/by-department/개발팀
```

## 모니터링

### Kafka UI
- URL: http://localhost:8080
- Topic: user-events 모니터링

### PostgreSQL 직접 접근
```bash
docker exec -it postgres psql -U postgres -d materialized_view_db

# 사용자 뷰 조회
SELECT * FROM user_view;

# 부서별 통계 조회
SELECT * FROM department_stats;
```

## 고급 사용법

### 1. 이벤트 재처리
```bash
# Kafka 이벤트 재처리
./scripts/kafka-replay.sh -t user-events -o earliest
```

### 2. 사용자 정보 업데이트
```bash
# 사용자 정보 업데이트 (자동으로 포스트에 반영)
curl -X PUT http://localhost:8081/api/users/1 \
  -H "Content-Type: application/json" \
  -d '{
    "name": "홍길동 수정",
    "email": "hong.updated@example.com",
    "department": "기획팀",
    "status": "ACTIVE"
  }'
```

### 3. 성능 최적화
- PostgreSQL 인덱스 최적화
- Kafka Streams 파티션 조정
- 배치 처리 설정

## 테스트

### 통합 테스트 실행
```bash
# post-service 테스트
cd post-service
./gradlew test

# user-service 테스트
cd user-service
./gradlew test
```

### 수동 테스트 시나리오
1. 사용자 생성 → Materialized View 확인
2. 포스트 생성 → 사용자 정보 자동 enrichment 확인
3. 사용자 수정 → 기존 포스트 정보 업데이트 확인
4. 사용자 삭제 → Materialized View 정리 확인

## 트러블슈팅

### 일반적인 문제
1. **Kafka 연결 오류**: docker-compose logs kafka
2. **PostgreSQL 연결 오류**: docker-compose logs postgres
3. **Streams 처리 지연**: Kafka UI에서 lag 확인

### 데이터 일관성 문제
```bash
# Kafka Consumer Group 리셋
kafka-consumer-groups --bootstrap-server localhost:9092 \
  --group post-service-streams --reset-offsets --to-earliest \
  --topic user-events --execute
```

## 확장 가능성

### 1. 추가 Materialized View
- 포스트 통계 뷰
- 사용자 활동 뷰
- 부서별 성과 뷰

### 2. CDC (Change Data Capture) 추가
```yaml
# Debezium Connector 추가
debezium:
  image: debezium/connect:latest
  depends_on:
    - kafka
    - postgres
```

### 3. KSQLDB 통합
```yaml
# KSQLDB 서버 추가
ksqldb-server:
  image: confluentinc/ksqldb-server:latest
  depends_on:
    - kafka
```

## 리소스

- [Kafka Streams 공식 문서](https://kafka.apache.org/documentation/streams/)
- [PostgreSQL Materialized View](https://www.postgresql.org/docs/current/rules-materializedviews.html)
- [Spring Kafka Streams](https://docs.spring.io/spring-kafka/docs/current/reference/html/#kafka-streams) 