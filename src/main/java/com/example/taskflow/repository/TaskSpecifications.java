package com.example.taskflow.repository;

import org.springframework.data.jpa.domain.Specification;

import com.example.taskflow.entity.Priority;
import com.example.taskflow.entity.Task;
import com.example.taskflow.entity.TaskStatus;

public class TaskSpecifications {
	
	private TaskSpecifications() {}
	
	public static Specification<Task> inProject(Long projectId) {
		return (root, query, cb) -> cb.equal(root.get("project").get("id"), projectId);
	}
	
	public static Specification<Task> hasStatus(TaskStatus status) {
		return (root, query, cb) -> cb.equal(root.get("status"), status);
	}
	
	public static Specification<Task> hasPriority(Priority priority) {
		return (root, query, cb) -> cb.equal(root.get("priority"), priority);
	}
	
	public static Specification<Task> titleContains(String text) {
	    return (root, query, cb) ->
	            cb.like(cb.lower(root.get("title")), "%" + text.toLowerCase() + "%");
	}
	
	
	
}
