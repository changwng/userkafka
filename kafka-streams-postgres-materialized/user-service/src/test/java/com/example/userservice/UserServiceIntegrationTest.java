package com.example.userservice;

import com.example.userservice.domain.User;
import com.example.userservice.domain.UserEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EmbeddedKafka(partitions = 1, topics = "user-events")
@Testcontainers
class UserServiceIntegrationTest {

    @LocalServerPort
    int port;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafka;

    private RestTemplate restTemplate = new RestTemplate();
    private ObjectMapper objectMapper = new ObjectMapper();

    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", () -> "localhost:9092");
    }

    @Test
    void 사용자_생성시_Kafka_이벤트_발행() throws Exception {
        // given
        User user = User.builder()
                .name("테스트")
                .email("test@example.com")
                .department("개발팀")
                .status("ACTIVE")
                .build();

        // when
        User created = restTemplate.postForObject("http://localhost:" + port + "/api/users", user, User.class);

        // then
        Properties props = new Properties();
        props.put("bootstrap.servers", embeddedKafka.getBrokersAsString());
        props.put("group.id", "test-group");
        props.put("key.deserializer", StringDeserializer.class.getName());
        props.put("value.deserializer", StringDeserializer.class.getName());
        props.put("auto.offset.reset", "earliest");

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(Collections.singleton("user-events"));
            ConsumerRecord<String, String> record = consumer.poll(Duration.ofSeconds(5)).iterator().next();
            UserEvent event = objectMapper.readValue(record.value(), UserEvent.class);

            assertThat(event.getUser().getName()).isEqualTo("테스트");
            assertThat(event.getEventType()).isEqualTo("USER_CREATED");
        }
    }
} 