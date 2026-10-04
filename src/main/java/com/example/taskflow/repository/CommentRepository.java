package com.example.taskflow.repository;

import com.example.taskflow.entity.Comment;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    long countByTaskId(Long taskId);

    // One query for a whole page of tasks: [taskId, count] rows
    @Query("""
            select c.task.id, count(c)
            from Comment c
            where c.task.id in :taskIds
            group by c.task.id
            """)
    List<Object[]> countByTaskIds(@Param("taskIds") Collection<Long> taskIds);

    // @EntityGraph: load the author in the same SELECT (declarative fix)
    @EntityGraph(attributePaths = "author")
    List<Comment> findByTaskIdOrderByCreatedAtAscIdAsc(Long taskId);

    // FIXED: one SELECT with explicit join fetches
    @Query("""
            select c from Comment c
            join fetch c.author
            join fetch c.task t
            join fetch t.project p
            where p.owner.id = :ownerId
            order by c.createdAt desc, c.id desc
            """)
    List<Comment> findRecentForOwner(@Param("ownerId") Long ownerId, Pageable pageable);

    // NAIVE: kept only so you can measure the N+1 problem. Delete it after the experiment.
    @Query("""
            select c from Comment c
            where c.task.project.owner.id = :ownerId
            order by c.createdAt desc, c.id desc
            """)
    List<Comment> findRecentForOwnerNaive(@Param("ownerId") Long ownerId, Pageable pageable);
}