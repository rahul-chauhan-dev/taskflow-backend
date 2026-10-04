package com.example.taskflow.dto;

import com.example.taskflow.entity.Priority;
import com.example.taskflow.entity.Task;
import com.example.taskflow.entity.TaskStatus;

import java.time.LocalDate;

public record TaskResponse(
        Long id,
        String title,
        String description,
        TaskStatus status,
        Priority priority,
        LocalDate dueDate,
        Long projectId,
        long commentCount) {

    public static TaskResponse from(Task t, long commentCount) {
        return new TaskResponse(
                t.getId(), t.getTitle(), t.getDescription(),
                t.getStatus(), t.getPriority(), t.getDueDate(),
                t.getProject().getId(), commentCount);
    }
}