package com.example.taskflow.dto;

import com.example.taskflow.entity.Priority;
import com.example.taskflow.entity.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record TaskRequest(
        @NotBlank(message = "Title is required")
        @Size(max = 150, message = "Title must be at most 150 characters")
        String title,

        @Size(max = 255, message = "Description must be at most 255 characters")
        String description,

        TaskStatus status,
        Priority priority,
        LocalDate dueDate) {
}