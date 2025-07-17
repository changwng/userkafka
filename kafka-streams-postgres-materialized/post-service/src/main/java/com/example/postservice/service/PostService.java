package com.example.postservice.service;

import com.example.postservice.domain.Post;
import com.example.postservice.domain.UserView;
import com.example.postservice.repository.UserViewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostService {
    
    private final UserViewRepository userViewRepository;
    private final Map<Long, Post> postStore = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);
    
    public Post createPost(Post post) {
        Long id = idGenerator.getAndIncrement();
        post.setId(id);
        post.setCreatedAt(LocalDateTime.now());
        post.setUpdatedAt(LocalDateTime.now());
        
        // Enrich with user information from materialized view
        enrichPostWithUserInfo(post);
        
        postStore.put(id, post);
        log.info("Post created: {}", post);
        return post;
    }
    
    public Post updatePost(Long id, Post updatePost) {
        Post existingPost = postStore.get(id);
        if (existingPost == null) {
            throw new RuntimeException("Post not found: " + id);
        }
        
        updatePost.setId(id);
        updatePost.setCreatedAt(existingPost.getCreatedAt());
        updatePost.setUpdatedAt(LocalDateTime.now());
        
        // Enrich with user information from materialized view
        enrichPostWithUserInfo(updatePost);
        
        postStore.put(id, updatePost);
        log.info("Post updated: {}", updatePost);
        return updatePost;
    }
    
    public void deletePost(Long id) {
        Post post = postStore.remove(id);
        if (post == null) {
            throw new RuntimeException("Post not found: " + id);
        }
        log.info("Post deleted: {}", id);
    }
    
    public Post getPost(Long id) {
        Post post = postStore.get(id);
        if (post == null) {
            throw new RuntimeException("Post not found: " + id);
        }
        
        // Refresh user information from materialized view
        enrichPostWithUserInfo(post);
        return post;
    }
    
    public List<Post> getAllPosts() {
        return postStore.values().stream()
                .map(post -> {
                    // Refresh user information from materialized view
                    enrichPostWithUserInfo(post);
                    return post;
                })
                .collect(Collectors.toList());
    }
    
    public List<Post> getPostsByDepartment(String department) {
        List<UserView> users = userViewRepository.findByDepartment(department);
        List<Long> userIds = users.stream().map(UserView::getUserId).collect(Collectors.toList());
        
        return postStore.values().stream()
                .filter(post -> userIds.contains(post.getAuthorId()))
                .map(post -> {
                    enrichPostWithUserInfo(post);
                    return post;
                })
                .collect(Collectors.toList());
    }
    
    private void enrichPostWithUserInfo(Post post) {
        if (post.getAuthorId() != null) {
            Optional<UserView> userView = userViewRepository.findById(post.getAuthorId());
            if (userView.isPresent()) {
                UserView user = userView.get();
                post.setAuthorName(user.getName());
                post.setAuthorEmail(user.getEmail());
                post.setAuthorDepartment(user.getDepartment());
            } else {
                log.warn("User not found in materialized view: {}", post.getAuthorId());
            }
        }
    }
} 