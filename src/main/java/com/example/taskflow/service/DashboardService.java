package com.example.taskflow.service;

import com.example.taskflow.dto.ActivityItem;
import com.example.taskflow.dto.DashboardResponse;
import com.example.taskflow.dto.ProjectResponse;
import com.example.taskflow.dto.TaskTotals;
import com.example.taskflow.repository.CommentRepository;
import com.example.taskflow.repository.ProjectRepository;
import com.example.taskflow.repository.TaskRepository;
import com.example.taskflow.security.AuthUser;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class DashboardService {

    private static final int RECENT_LIMIT = 10;
    private static final int DUE_SOON_DAYS = 7;

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final CommentRepository commentRepository;

    public DashboardService(TaskRepository taskRepository, ProjectRepository projectRepository,
                            CommentRepository commentRepository) {
        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
        this.commentRepository = commentRepository;
    }

    @Transactional(readOnly = true)
    public DashboardResponse get(AuthUser user) {
        LocalDate today = LocalDate.now();

        TaskTotals totals = taskRepository.totalsForOwner(
                user.id(), today, today.plusDays(DUE_SOON_DAYS));

        List<ProjectResponse> projects = projectRepository.findAllWithCounts(user.id());

        List<ActivityItem> activity = commentRepository
                .findRecentForOwner(user.id(), PageRequest.of(0, RECENT_LIMIT))
                .stream()
                .map(ActivityItem::from)
                .toList();

        return new DashboardResponse(
                projects.size(),
                totals.total(), totals.todo(), totals.inProgress(), totals.done(),
                totals.overdue(), totals.dueSoon(), totals.highPriorityOpen(),
                projects, activity);
    }
}