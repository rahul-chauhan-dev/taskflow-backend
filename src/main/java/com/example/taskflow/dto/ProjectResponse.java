package com.example.taskflow.dto;

import com.example.taskflow.entity.Project;
import java.time.LocalDateTime;

public record ProjectResponse(
        Long id,
        String name,
        String description,
        LocalDateTime createdAt,
        Long taskCount,
        Long todoCount,
        Long inProgressCount,
        Long doneCount) {

    public static ProjectResponse forNewProject(Project p) {
        return new ProjectResponse(
                p.getId(), p.getName(), p.getDescription(), p.getCreatedAt(),
                0L, 0L, 0L, 0L);
    }
}