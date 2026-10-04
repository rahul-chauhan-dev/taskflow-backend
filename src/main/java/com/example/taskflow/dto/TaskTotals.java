package com.example.taskflow.dto;

// Result row of the dashboard aggregate query. Long (not long) so Hibernate's constructor matching succeeds.
public record TaskTotals(
        Long total,
        Long todo,
        Long inProgress,
        Long done,
        Long overdue,
        Long dueSoon,
        Long highPriorityOpen) {
}