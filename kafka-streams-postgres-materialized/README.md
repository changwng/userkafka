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

    curl -X POST http://localhost:8081/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "name": "last_장웅",
    "email": "last_changwng@example.com",
    "department": "개발팀",
    "status": "ACTIVE"
  }'
  
   curl -X GET http://localhost:8081/api/users/7 \
  -H "Content-Type: application/json" 
   
  curl -X PUT http://localhost:8081/api/users/7 \
  -H "Content-Type: application/json" \
  -d  '{"id":7,"name":"modi_장웅","email":"last_changwng@example.com","department":"개발팀","status":"ACTIVE","createdAt":"2025-07-17T06:03:43.792924968","updatedAt":"2025-07-17T06:03:43.793003009"}'

curl -X DELETE http://localhost:8081/api/users/7 \
  -H "Content-Type: application/json" 

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

docker exec -it kafka kafka-topics --help

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

### post-service의 사용자 삭제후 kafka에서 다시 받는 방법 ( offset을 리셋으로 만들기)
러의 의미: current state is Stable 피해가기
1. post-service down
2. user-service down : 이유 post-service-streams 컨슈머 그룹을 inactive 상태로  user-events를 보내고 받는곳을 다 재외후에 offset을  
3. ./scripts/kafka-replay.sh -t user-events -r post-service-streams 실행하면 유저 재생성됨
4. post-service start
5. user-service start
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


consumer group 이름이 다르면:
각 서비스는 모든 메시지를 독립적으로 소비(처음부터 읽거나, 원하는 오프셋부터 읽기 가능)
consumer group 이름이 같으면:
여러 인스턴스가 하나의 그룹으로 묶여 파티션을 분할해서 읽음(로드밸런싱, 메시지 중복 소비 없음)

### 50만명 데이터 생성시간 측정

select count(*) from user_view;
184793 으로 디비 연계가 같이 안돌아감 ... 그래서 튜닝이 필요함

452000명 생성 완료 (경과 시간: 1446.47초, 마지막 응답: 200)


💡 대략적인 Kafka 메시지 초당 처리량 기준
경 수준
초당 메시지 수
설명
개발/테스트 환경
100 ~ 1,000 msg/sec
간단한 개발, 로깅 또는 기능 테스트 수준
소규모 서비스 운영 환경
1,000 ~ 10,000 msg/sec
소규모 서비스, 마이크로서비스 연동 수준
중규모 운영 환경
10,000 ~ 100,000 msg/sec
다수의 마이크로서비스와 실시간 로그/이벤트 수집
대규모 운영 환경
100,000 ~ 1,000,000 msg/sec
스트리밍 분석, IoT, 클릭스트림 분석 등
초대형 기업/전문 인프라
수백만 msg/sec
Netflix, LinkedIn 등 카프카 내부 구조 확장 활용

2025-07-17 08:42:16,294] ERROR Error while loading log dir /var/lib/kafka/data (kafka.server.LogDirFailureChannel)

java.io.IOException: No space left on device

	at java.base/sun.nio.ch.FileDispatcherImpl.write0(Native Method)

	at java.base/sun.nio.ch.FileDispatcherImpl.write(FileDispatcherImpl.java:62)

	at java.base/sun.nio.ch.IOUtil.writeFromNativeBuffer(IOUtil.java:113)

	at java.base/sun.nio.ch.IOUtil.write(IOUtil.java:79)

	at java.base/sun.nio.ch.FileChannelImpl.write(FileChannelImpl.java:280)

	at kafka.log.ProducerStateManager$.kafka$log$ProducerStateManager$$writeSnapshot(ProducerStateManager.scala:451)

	at kafka.log.ProducerStateManager.takeSnapshot(ProducerStateManager.scala:755)

	at kafka.log.LogLoader.recoverSegment(LogLoader.scala:375)

	at kafka.log.LogLoader.recoverLog(LogLoader.scala:427)

	at kafka.log.LogLoader.load(LogLoader.scala:165)

	at kafka.log.UnifiedLog$.apply(UnifiedLog.scala:1853)

	at kafka.log.LogManager.loadLog(LogManager.scala:287)

	at kafka.log.LogManager.$anonfun$loadLogs$14(LogManager.scala:403)

	at java.base/java.util.concurrent.Executors$RunnableAdapter.call(Executors.java:515)

	at java.base/java.util.concurrent.FutureTask.run(FutureTask.java:264)