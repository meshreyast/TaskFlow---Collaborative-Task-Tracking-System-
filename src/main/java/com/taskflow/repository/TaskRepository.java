package com.taskflow.repository;

import com.taskflow.domain.Task;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {

    /** Fetches the to-one associations in the same query so mapping a page does not cause N+1 selects. */
    @Override
    @EntityGraph(attributePaths = {"createdBy", "assignee", "team"})
    Page<Task> findAll(Specification<Task> spec, Pageable pageable);

    List<Task> findAllByTeamId(Long teamId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Task t set t.assignee = null where t.team.id = :teamId and t.assignee.id = :userId")
    int unassignInTeam(@Param("teamId") Long teamId, @Param("userId") Long userId);
}
