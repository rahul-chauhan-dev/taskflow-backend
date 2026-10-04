package com.example.taskflow.repository;

import com.example.taskflow.dto.TaskTotals;
import com.example.taskflow.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface TaskRepository
        extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {

    List<Task> findByProjectId(Long projectId);

    long countByProjectId(Long projectId);

    @Query("""
            select new com.example.taskflow.dto.TaskTotals(
                count(t),
                count(case when t.status = com.example.taskflow.entity.TaskStatus.TODO then 1 end),
                count(case when t.status = com.example.taskflow.entity.TaskStatus.IN_PROGRESS then 1 end),
                count(case when t.status = com.example.taskflow.entity.TaskStatus.DONE then 1 end),
                count(case when t.status <> com.example.taskflow.entity.TaskStatus.DONE
                           and t.dueDate < :today then 1 end),
                count(case when t.status <> com.example.taskflow.entity.TaskStatus.DONE
                           and t.dueDate >= :today and t.dueDate <= :soon then 1 end),
                count(case when t.status <> com.example.taskflow.entity.TaskStatus.DONE
                           and t.priority = com.example.taskflow.entity.Priority.HIGH then 1 end))
            from Task t
            where t.project.owner.id = :ownerId
            """)
    TaskTotals totalsForOwner(@Param("ownerId") Long ownerId,
                              @Param("today") LocalDate today,
                              @Param("soon") LocalDate soon);
}