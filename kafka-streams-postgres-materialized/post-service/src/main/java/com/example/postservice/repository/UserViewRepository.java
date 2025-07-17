package com.example.postservice.repository;

import com.example.postservice.domain.UserView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserViewRepository extends JpaRepository<UserView, Long> {
    
    @Query("SELECT uv FROM UserView uv WHERE uv.department = :department")
    List<UserView> findByDepartment(@Param("department") String department);
    
    @Query("SELECT uv FROM UserView uv WHERE uv.status = :status")
    List<UserView> findByStatus(@Param("status") String status);
    
    @Query("SELECT uv FROM UserView uv WHERE uv.name LIKE %:name%")
    List<UserView> findByNameContaining(@Param("name") String name);
} 