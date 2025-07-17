package com.example.postservice.controller;

import com.example.postservice.domain.Post;
import com.example.postservice.domain.UserView;
import com.example.postservice.service.PostService;
import com.example.postservice.repository.UserViewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {
    
    private final PostService postService;
    private final UserViewRepository userViewRepository;
    
    @PostMapping
    public ResponseEntity<Post> createPost(@RequestBody Post post) {
        Post created = postService.createPost(post);
        return ResponseEntity.ok(created);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<Post> updatePost(@PathVariable Long id, @RequestBody Post post) {
        Post updated = postService.updatePost(id, post);
        return ResponseEntity.ok(updated);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePost(@PathVariable Long id) {
        postService.deletePost(id);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Post> getPost(@PathVariable Long id) {
        Post post = postService.getPost(id);
        return ResponseEntity.ok(post);
    }
    
    @GetMapping
    public ResponseEntity<List<Post>> getAllPosts() {
        List<Post> posts = postService.getAllPosts();
        return ResponseEntity.ok(posts);
    }
    
    @GetMapping("/by-department/{department}")
    public ResponseEntity<List<Post>> getPostsByDepartment(@PathVariable String department) {
        List<Post> posts = postService.getPostsByDepartment(department);
        return ResponseEntity.ok(posts);
    }
    
    @GetMapping("/users")
    public ResponseEntity<List<UserView>> getAllUsers() {
        List<UserView> users = userViewRepository.findAll();
        return ResponseEntity.ok(users);
    }
    
    @GetMapping("/users/department/{department}")
    public ResponseEntity<List<UserView>> getUsersByDepartment(@PathVariable String department) {
        List<UserView> users = userViewRepository.findByDepartment(department);
        return ResponseEntity.ok(users);
    }
} 