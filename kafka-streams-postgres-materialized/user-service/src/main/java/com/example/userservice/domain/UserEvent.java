package com.example.userservice.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserEvent {
    @JsonProperty("eventType")
    private String eventType; // USER_CREATED, USER_UPDATED, USER_DELETED
    
    @JsonProperty("userId")
    private Long userId;
    
    @JsonProperty("user")
    private User user;
    
    @JsonProperty("timestamp")
    private LocalDateTime timestamp;
    
    @JsonProperty("version")
    private Long version;
} 