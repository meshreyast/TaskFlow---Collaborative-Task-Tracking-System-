package com.taskflow.repository;

import com.taskflow.domain.Attachment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AttachmentRepository extends JpaRepository<Attachment, Long> {

    @EntityGraph(attributePaths = "uploadedBy")
    List<Attachment> findAllByTaskIdOrderByCreatedAtAsc(Long taskId);

    List<Attachment> findAllByTaskId(Long taskId);

    @Modifying(flushAutomatically = true)
    @Query("delete from Attachment a where a.task.id = :taskId")
    void deleteAllByTaskId(@Param("taskId") Long taskId);
}
