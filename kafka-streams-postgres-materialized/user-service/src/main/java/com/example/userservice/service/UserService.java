package com.example.userservice.service;

import com.example.userservice.domain.User;
import com.example.userservice.domain.UserEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final Map<Long, User> userStore = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);
    
    private static final String USER_EVENTS_TOPIC = "user-events";
    
    public User createUser(User user) {
        Long id = idGenerator.getAndIncrement();
        user.setId(id);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        
        userStore.put(id, user);
        
        UserEvent event = UserEvent.builder()
                .eventType("USER_CREATED")
                .userId(id)
                .user(user)
                .timestamp(LocalDateTime.now())
                .version(1L)
                .build();
        
        publishUserEvent(event);
        
        log.info("User created: {}", user);
        return user;
    }
    
    public User updateUser(Long id, User updateUser) {
        User existingUser = userStore.get(id);
        if (existingUser == null) {
            throw new RuntimeException("User not found: " + id);
        }
        
        updateUser.setId(id);
        updateUser.setCreatedAt(existingUser.getCreatedAt());
        updateUser.setUpdatedAt(LocalDateTime.now());
        
        userStore.put(id, updateUser);
        
        UserEvent event = UserEvent.builder()
                .eventType("USER_UPDATED")
                .userId(id)
                .user(updateUser)
                .timestamp(LocalDateTime.now())
                .version(2L)
                .build();
        
        publishUserEvent(event);
        
        log.info("User updated: {}", updateUser);
        return updateUser;
    }
    
    public void deleteUser(Long id) {
        User user = userStore.remove(id);
        if (user == null) {
            throw new RuntimeException("User not found: " + id);
        }
        
        UserEvent event = UserEvent.builder()
                .eventType("USER_DELETED")
                .userId(id)
                .user(user)
                .timestamp(LocalDateTime.now())
                .version(3L)
                .build();
        
        publishUserEvent(event);
        
        log.info("User deleted: {}", id);
    }
    
    public User getUser(Long id) {
        User user = userStore.get(id);
        if (user == null) {
            throw new RuntimeException("User not found: " + id);
        }
        return user;
    }
    
    public List<User> getAllUsers() {
        return List.copyOf(userStore.values());
    }
    
    private void publishUserEvent(UserEvent event) {
        try {
            String eventJson = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(USER_EVENTS_TOPIC, event.getUserId().toString(), eventJson);
            log.info("Published user event: {}", event.getEventType());
        } catch (Exception e) {
            log.error("Failed to publish user event", e);
        }
    }
} 