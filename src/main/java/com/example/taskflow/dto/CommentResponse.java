package com.example.taskflow.dto;

import com.example.taskflow.entity.Comment;

import java.time.LocalDateTime;

public record CommentResponse(
        Long id,
        String content,
        LocalDateTime createdAt,
        Long taskId,
        Long authorId,
        String authorName) {

    public static CommentResponse from(Comment c) {
        return new CommentResponse(
                c.getId(), c.getContent(), c.getCreatedAt(),
                c.getTask().getId(),
                c.getAuthor().getId(), c.getAuthor().getName());
    }
}