package com.example.taskflow.dto;

import com.example.taskflow.entity.Comment;
import com.example.taskflow.entity.Project;
import com.example.taskflow.entity.Task;

import java.time.LocalDateTime;

public record ActivityItem(
        Long commentId,
        String content,
        LocalDateTime createdAt,
        String authorName,
        Long taskId,
        String taskTitle,
        Long projectId,
        String projectName) {

    public static ActivityItem from(Comment c) {
        Task t = c.getTask();
        Project p = t.getProject();
        return new ActivityItem(
                c.getId(), c.getContent(), c.getCreatedAt(), c.getAuthor().getName(),
                t.getId(), t.getTitle(), p.getId(), p.getName());
    }
}