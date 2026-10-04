package com.example.taskflow.dto;

import java.util.List;

public record DashboardResponse(
        long projectCount,
        long totalTasks,
        long todoCount,
        long inProgressCount,
        long doneCount,
        long overdueCount,
        long dueSoonCount,
        long highPriorityOpenCount,
        List<ProjectResponse> projects,
        List<ActivityItem> recentActivity) {
}