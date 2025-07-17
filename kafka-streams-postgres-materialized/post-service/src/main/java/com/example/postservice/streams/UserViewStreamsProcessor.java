package com.example.postservice.streams;

import com.example.postservice.domain.UserEvent;
import com.example.postservice.domain.UserView;
import com.example.postservice.repository.UserViewRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserViewStreamsProcessor {
    
    private final UserViewRepository userViewRepository;
    private final ObjectMapper objectMapper;
    
    @Autowired
    public void buildPipeline(StreamsBuilder streamsBuilder) {
        KStream<String, String> userEventsStream = streamsBuilder
                .stream("user-events", Consumed.with(Serdes.String(), Serdes.String()));
        log.info("userEventsStream: {}", userEventsStream);
        userEventsStream.foreach((key, value) -> {
            try {
                UserEvent event = objectMapper.readValue(value, UserEvent.class);
                log.info("event: {}", event);
                processUserEvent(event);
            } catch (Exception e) {
                log.error("Error processing user event: {}", value, e);
            }
        });
        
        log.info("User view streams processor initialized");
    }
    
    private void processUserEvent(UserEvent event) {
        log.info("Processing user event: {} for user: {}", event.getEventType(), event.getUserId());
        
        try {
            switch (event.getEventType()) {
                case "USER_CREATED":
                case "USER_UPDATED":
                    upsertUserView(event);
                    break;
                case "USER_DELETED":
                    deleteUserView(event);
                    break;
                default:
                    log.warn("Unknown event type: {}", event.getEventType());
            }
        } catch (Exception e) {
            log.error("Error processing user event: {}", event, e);
        }
    }
    
    private void upsertUserView(UserEvent event) {
        UserView userView = UserView.builder()
                .userId(event.getUserId())
                .name(event.getUser().getName())
                .email(event.getUser().getEmail())
                .department(event.getUser().getDepartment())
                .status(event.getUser().getStatus())
                .createdAt(event.getUser().getCreatedAt())
                .updatedAt(event.getUser().getUpdatedAt())
                .version(event.getVersion())
                .lastProcessedAt(LocalDateTime.now())
                .build();
        
        userViewRepository.save(userView);
        log.info("User view updated for user: {}", event.getUserId());
    }
    
    private void deleteUserView(UserEvent event) {
        userViewRepository.deleteById(event.getUserId());
        log.info("User view deleted for user: {}", event.getUserId());
    }
} 