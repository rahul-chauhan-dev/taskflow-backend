package com.example.taskflow.controller;

import com.example.taskflow.dto.PageResponse;
import com.example.taskflow.dto.TaskRequest;
import com.example.taskflow.dto.TaskResponse;
import com.example.taskflow.dto.TaskStatusRequest;
import com.example.taskflow.entity.Priority;
import com.example.taskflow.entity.TaskStatus;
import com.example.taskflow.security.AuthUser;
import com.example.taskflow.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping("/projects/{projectId}/tasks")
    public PageResponse<TaskResponse> search(
            @PathVariable Long projectId,
            @AuthenticationPrincipal AuthUser user,
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) Priority priority,
            @RequestParam(required = false) String q,
            @PageableDefault(size = 5, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return taskService.search(projectId, user, status, priority, q, pageable);
    }

    @PostMapping("/projects/{projectId}/tasks")
    @ResponseStatus(HttpStatus.CREATED)
    public TaskResponse create(@PathVariable Long projectId, @AuthenticationPrincipal AuthUser user,
                               @Valid @RequestBody TaskRequest request) {
        return taskService.create(projectId, user, request);
    }

    @PutMapping("/tasks/{id}")
    public TaskResponse update(@PathVariable Long id, @AuthenticationPrincipal AuthUser user,
                               @Valid @RequestBody TaskRequest request) {
        return taskService.update(id, user, request);
    }

    @PatchMapping("/tasks/{id}/status")
    public TaskResponse updateStatus(@PathVariable Long id, @AuthenticationPrincipal AuthUser user,
                                     @Valid @RequestBody TaskStatusRequest request) {
        return taskService.updateStatus(id, user, request.status());
    }

    @DeleteMapping("/tasks/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @AuthenticationPrincipal AuthUser user) {
        taskService.delete(id, user);
    }
}