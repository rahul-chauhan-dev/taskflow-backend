package com.example.taskflow.service;

import com.example.taskflow.dto.PageResponse;
import com.example.taskflow.dto.TaskRequest;
import com.example.taskflow.dto.TaskResponse;
import com.example.taskflow.entity.Priority;
import com.example.taskflow.entity.Project;
import com.example.taskflow.entity.Task;
import com.example.taskflow.entity.TaskStatus;
import com.example.taskflow.exception.BadRequestException;
import com.example.taskflow.exception.ResourceNotFoundException;
import com.example.taskflow.repository.CommentRepository;
import com.example.taskflow.repository.TaskRepository;
import com.example.taskflow.repository.TaskSpecifications;
import com.example.taskflow.security.AuthUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class TaskService {

    private static final Set<String> SORTABLE_FIELDS = Set.of("id", "title", "dueDate");

    private final TaskRepository taskRepository;
    private final CommentRepository commentRepository;
    private final ProjectService projectService;

    public TaskService(TaskRepository taskRepository, CommentRepository commentRepository,
                       ProjectService projectService) {
        this.taskRepository = taskRepository;
        this.commentRepository = commentRepository;
        this.projectService = projectService;
    }

    @Transactional(readOnly = true)
    public PageResponse<TaskResponse> search(Long projectId, AuthUser user, TaskStatus status,
                                             Priority priority, String q, Pageable pageable) {
        projectService.getOwned(projectId, user); // 404 if missing or not yours

        for (Sort.Order order : pageable.getSort()) {
            if (!SORTABLE_FIELDS.contains(order.getProperty())) {
                throw new BadRequestException(
                        "Cannot sort by '" + order.getProperty() + "'. Allowed: " + SORTABLE_FIELDS);
            }
        }

        Sort sort = pageable.getSort().and(Sort.by("id"));
        Pageable stablePageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);

        Specification<Task> spec = TaskSpecifications.inProject(projectId);
        if (status != null) {
            spec = spec.and(TaskSpecifications.hasStatus(status));
        }
        if (priority != null) {
            spec = spec.and(TaskSpecifications.hasPriority(priority));
        }
        if (q != null && !q.isBlank()) {
            spec = spec.and(TaskSpecifications.titleContains(q.trim()));
        }

        Page<Task> page = taskRepository.findAll(spec, stablePageable);

        // ONE extra query for the whole page, instead of one per task
        List<Long> ids = page.getContent().stream().map(Task::getId).toList();
        Map<Long, Long> counts = commentCounts(ids);

        return PageResponse.from(
                page.map(t -> TaskResponse.from(t, counts.getOrDefault(t.getId(), 0L))));
    }

    @Transactional
    public TaskResponse create(Long projectId, AuthUser user, TaskRequest request) {
        Project project = projectService.getOwned(projectId, user);
        Task task = new Task();
        applyRequest(task, request);
        task.setProject(project);
        return TaskResponse.from(taskRepository.save(task), 0);
    }

    @Transactional
    public TaskResponse update(Long id, AuthUser user, TaskRequest request) {
        Task task = findOwnedTask(id, user);
        applyRequest(task, request);
        Task saved = taskRepository.save(task);
        return TaskResponse.from(saved, commentRepository.countByTaskId(id));
    }

    @Transactional
    public TaskResponse updateStatus(Long id, AuthUser user, TaskStatus status) {
        Task task = findOwnedTask(id, user);
        task.setStatus(status); // dirty checking writes the UPDATE at commit
        return TaskResponse.from(task, commentRepository.countByTaskId(id));
    }

    @Transactional
    public void delete(Long id, AuthUser user) {
        taskRepository.delete(findOwnedTask(id, user)); // cascades to its comments
    }

    // Used by CommentService: returns the task only if the user owns its project
    @Transactional(readOnly = true)
    public Task getOwnedTask(Long id, AuthUser user) {
        return findOwnedTask(id, user);
    }

    private Task findOwnedTask(Long id, AuthUser user) {
        Task task = taskRepository.findById(id).orElseThrow(() -> taskNotFound(id));
        if (!projectService.isOwner(task.getProject(), user)) {
            throw taskNotFound(id);
        }
        return task;
    }

    private Map<Long, Long> commentCounts(Collection<Long> taskIds) {
        if (taskIds.isEmpty()) {
            return Map.of(); // never send an empty IN () list to the database
        }
        Map<Long, Long> counts = new HashMap<>();
        for (Object[] row : commentRepository.countByTaskIds(taskIds)) {
            counts.put((Long) row[0], (Long) row[1]);
        }
        return counts;
    }

    private static ResourceNotFoundException taskNotFound(Long id) {
        return new ResourceNotFoundException("Task not found with id " + id);
    }

    private void applyRequest(Task task, TaskRequest r) {
        task.setTitle(r.title());
        task.setDescription(r.description());
        task.setStatus(r.status() != null ? r.status() : TaskStatus.TODO);
        task.setPriority(r.priority() != null ? r.priority() : Priority.MEDIUM);
        task.setDueDate(r.dueDate());
    }
}