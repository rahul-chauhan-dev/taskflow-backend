package com.example.taskflow.controller;

import com.example.taskflow.dto.CommentRequest;
import com.example.taskflow.dto.CommentResponse;
import com.example.taskflow.security.AuthUser;
import com.example.taskflow.service.CommentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping("/tasks/{taskId}/comments")
    public List<CommentResponse> list(@PathVariable Long taskId,
                                      @AuthenticationPrincipal AuthUser user) {
        return commentService.list(taskId, user);
    }

    @PostMapping("/tasks/{taskId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse add(@PathVariable Long taskId,
                               @AuthenticationPrincipal AuthUser user,
                               @Valid @RequestBody CommentRequest request) {
        return commentService.add(taskId, user, request);
    }

    @DeleteMapping("/comments/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @AuthenticationPrincipal AuthUser user) {
        commentService.delete(id, user);
    }
}