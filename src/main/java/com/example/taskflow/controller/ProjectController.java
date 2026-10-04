package com.example.taskflow.controller;

import com.example.taskflow.dto.ProjectRequest;
import com.example.taskflow.dto.ProjectResponse;
import com.example.taskflow.security.AuthUser;
import com.example.taskflow.service.ProjectService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping
    public List<ProjectResponse> getAll(@AuthenticationPrincipal AuthUser user) {
        return projectService.getAll(user);
    }

    @GetMapping("/search")
    public List<ProjectResponse> search(@AuthenticationPrincipal AuthUser user,
                                        @RequestParam String name) {
        return projectService.searchByName(user, name);
    }

    @GetMapping("/{id}")
    public ProjectResponse getById(@PathVariable Long id, @AuthenticationPrincipal AuthUser user) {
        return projectService.getResponseById(id, user);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectResponse create(@AuthenticationPrincipal AuthUser user,
                                  @Valid @RequestBody ProjectRequest request) {
        return projectService.create(user, request);
    }

    @PutMapping("/{id}")
    public ProjectResponse update(@PathVariable Long id, @AuthenticationPrincipal AuthUser user,
                                  @Valid @RequestBody ProjectRequest request) {
        return projectService.update(id, user, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @AuthenticationPrincipal AuthUser user) {
        projectService.delete(id, user);
    }
}