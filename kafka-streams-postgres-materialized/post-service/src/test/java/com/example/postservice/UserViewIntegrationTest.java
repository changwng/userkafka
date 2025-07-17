package com.example.postservice;

import com.example.postservice.domain.UserEvent;
import com.example.postservice.domain.UserView;
import com.example.postservice.repository.UserViewRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = "user-events")
@Testcontainers
class UserViewIntegrationTest {

    @Autowired
    private EmbeddedKafkaBroker embeddedKafka;

    @Autowired
    private UserViewRepository userViewRepository;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("materialized_view_db")
            .withUsername("postgres")
            .withPassword("password");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", () -> "localhost:9092");
    }

    @Test
    void Kafka_UserEvent_수신시_UserView_업데이트() throws Exception {
        // given
        UserEvent.User user = UserEvent.User.builder()
                .id(100L)
                .name("Kafka유저")
                .email("kafkauser@example.com")
                .department("테스트팀")
                .status("ACTIVE")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        UserEvent event = UserEvent.builder()
                .eventType("USER_CREATED")
                .userId(100L)
                .user(user)
                .timestamp(LocalDateTime.now())
                .version(1L)
                .build();

        Properties props = new Properties();
        props.put("bootstrap.servers", embeddedKafka.getBrokersAsString());
        props.put("key.serializer", StringSerializer.class.getName());
        props.put("value.serializer", StringSerializer.class.getName());

        try (KafkaProducer<String, String> producer = new KafkaProducer<>(props)) {
            producer.send(new ProducerRecord<>("user-events", "100", objectMapper.writeValueAsString(event)));
        }

        // when
        Thread.sleep(3000); // Kafka Streams 반영 대기

        // then
        UserView userView = userViewRepository.findById(100L).orElse(null);
        assertThat(userView).isNotNull();
        assertThat(userView.getName()).isEqualTo("Kafka유저");
        assertThat(userView.getEmail()).isEqualTo("kafkauser@example.com");
    }
} 