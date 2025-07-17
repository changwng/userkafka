package com.example.postservice.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_view")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserView {
    
    @Id
    @Column(name = "user_id")
    private Long userId;
    
    @Column(name = "name")
    private String name;
    
    @Column(name = "email")
    private String email;
    
    @Column(name = "department")
    private String department;
    
    @Column(name = "status")
    private String status;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "version")
    private Long version;
    
    @Column(name = "last_processed_at")
    private LocalDateTime lastProcessedAt;
} 