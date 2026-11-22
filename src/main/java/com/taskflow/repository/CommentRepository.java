package com.taskflow.repository;

import com.taskflow.domain.Comment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    @EntityGraph(attributePaths = "author")
    List<Comment> findAllByTaskIdOrderByCreatedAtAsc(Long taskId);

    @Modifying(flushAutomatically = true)
    @Query("delete from Comment c where c.task.id = :taskId")
    void deleteAllByTaskId(@Param("taskId") Long taskId);
}
