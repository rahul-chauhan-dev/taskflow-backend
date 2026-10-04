package com.example.taskflow.repository;

import com.example.taskflow.dto.ProjectResponse;
import com.example.taskflow.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    String COUNTS_SELECT = """
            select new com.example.taskflow.dto.ProjectResponse(
                p.id, p.name, p.description, p.createdAt,
                count(t),
                sum(case when t.status = com.example.taskflow.entity.TaskStatus.TODO then 1 else 0 end),
                sum(case when t.status = com.example.taskflow.entity.TaskStatus.IN_PROGRESS then 1 else 0 end),
                sum(case when t.status = com.example.taskflow.entity.TaskStatus.DONE then 1 else 0 end))
            from Project p
            left join p.tasks t
            """;

    String COUNTS_GROUP = """
            group by p.id, p.name, p.description, p.createdAt
            order by p.id
            """;

    @Query(COUNTS_SELECT + "where p.owner.id = :ownerId " + COUNTS_GROUP)
    List<ProjectResponse> findAllWithCounts(@Param("ownerId") Long ownerId);

    @Query(COUNTS_SELECT
            + "where p.owner.id = :ownerId and lower(p.name) like lower(concat('%', :name, '%')) "
            + COUNTS_GROUP)
    List<ProjectResponse> searchWithCounts(@Param("ownerId") Long ownerId, @Param("name") String name);

    @Query(COUNTS_SELECT + "where p.id = :id " + COUNTS_GROUP)
    Optional<ProjectResponse> findOneWithCounts(@Param("id") Long id);

    // Names only have to be unique per owner now
    boolean existsByOwnerIdAndNameIgnoreCase(Long ownerId, String name);

    boolean existsByOwnerIdAndNameIgnoreCaseAndIdNot(Long ownerId, String name, Long id);
}