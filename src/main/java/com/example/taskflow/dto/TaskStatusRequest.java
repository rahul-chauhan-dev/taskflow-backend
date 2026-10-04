package com.example.taskflow.dto;

import com.example.taskflow.entity.TaskStatus;
import jakarta.validation.constraints.NotNull;

public record TaskStatusRequest(
        @NotNull(message = "Status is required")
        TaskStatus status) {
}