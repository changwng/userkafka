### Offset Lag 줄이려면 batch + 병렬 처리 (멀티 스레드/멀티 인스턴스) + 최적화 조합이 필요
### 필요하다면 Kafka Streams 기반의 병렬 처리, 또는 Consumer Group scaling 전략도 정리해드릴 수 있어요.

✅ 목표: Offset Lag 줄이기 위한 3대 전략
1.	Batch 처리 (poll 단위 처리량 증가)
2.	병렬 처리 (멀티 스레드 or 멀티 인스턴스)
3.	처리 로직 최적화 (I/O 병목 제거 등)
 
1. Kafka는 poll()을 통해 다수의 레코드를 한 번에 가져올 수 있습니다.

🔧 설정 (Spring Kafka / Kafka Streams 공통)
설정 키 설명 추천 값
max.poll.records
한 번에 가져올 레코드 수
500 ~ 1000
fetch.min.bytes
최소 가져올 데이터 크기
1048576 (1MB)
fetch.max.wait.ms
기다릴 최대 시간
500

```
  spring.kafka.consumer:
  max-poll-records: 500
  fetch-min-bytes: 1048576
  fetch-max-wait-ms: 500
```
✅ 이렇게 설정하면 500개 이상 모이면 bulk로 처리되므로 처리 효율이 좋아짐.
2️⃣ 병렬 처리 전략
2-1. 멀티 인스턴스 (Consumer Group Scaling)

Kafka에서 lag을 줄이는 가장 확실한 방법은 consumer 수를 늘려 파티션당 병렬 처리하는 것입니다.
•	N개의 파티션에 대해 M개의 consumer를 배정 → 최대 N개의 병렬 처리 가능
•	Kafka Streams나 Spring Kafka 모두 consumer group으로 scaling 가능
```shell
# 단순히 post-service 인스턴스를 3개 띄우면 3개 consumer가 병렬로 처리
docker-compose up --scale post-service-kafka=3
docker-compose up --build --scale post-service=3
```
✅ 단, 파티션 수보다 consumer 수가 많아도 효과는 없음 → 파티션 ≥ consumer 수가 되도록 조정 필요
2-2. 멀티 스레드 (in-process 병렬 처리)
Kafka Streams 기본은 싱글 쓰레드 per StreamThread, num.stream.threads를 늘려 병렬화 가능.
```shell
spring.kafka.streams:
  properties:
    num.stream.threads: 4
    
```
```shell
StreamsConfig config = new StreamsConfig(Map.of(
    StreamsConfig.NUM_STREAM_THREADS_CONFIG, 4
));
```
3️⃣ 처리 로직 최적화
처리 시간이 길면 lag이 증가하므로 다음을 점검합니다:
🔍 1. DB 처리 병목 제거
•	단건 insert → batch insert
•	JPA → JDBC batch insert 전환
•	connection pool 제한 점검 (HikariCP max-pool-size)
🔍 2. I/O 제거
•	API 호출 or 외부 시스템 의존 제거
•	blocking → non-blocking 처리 (WebClient, Reactor 등)
🔍 3. 메시지 크기 최적화
•	메시지 페이로드에 불필요한 필드 제거
•	Avro/Protobuf 압축 사용 고려
4️⃣ Kafka Streams 기반 구조 최적화 (추가)
Kafka Streams 내부에는 RocksDB state store 등이 사용되므로:
•	commit.interval.ms 줄이기 (기본 30초 → 5초 등)
•	buffered.records.per.partition 늘리기
•	processing.guarantee를 at_least_once로 바꾸면 더 빠르나 신중히 고려
```java
props.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, 5000);
props.put(StreamsConfig.BUFFERED_RECORDS_PER_PARTITION_CONFIG, 1000);
props.put(StreamsConfig.PROCESSING_GUARANTEE_CONFIG, StreamsConfig.AT_LEAST_ONCE);
```
5️⃣ Kafka 클러스터 설정도 점검
replication.factor 과도한 복제는 성능 저하
num.partitions 병렬성의 기본 단위 → 충분히 나누어야
log.retention.ms 오래된 메시지 유지 설정이 lag에 영향

✅ 실전 적용 순서 요약
1.	Kafka에 적절한 파티션 수 설정 (예: 8~12)
2.	각 컨슈머 인스턴스 또는 스레드 수를 파티션 수에 맞춤
3.	max.poll.records, fetch.min.bytes 등으로 batch 최적화
4.	DB 등 외부 연동 처리 로직의 병목 제거
5.	Kafka Streams 옵션 조정 (thread 수, commit 간격, buffer 크기)
6.	Offset Lag 모니터링 → Kafka UI, Prometheus, Grafana 활용