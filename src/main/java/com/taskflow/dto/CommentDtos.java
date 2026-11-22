package com.taskflow.dto;

import com.taskflow.domain.Comment;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class CommentDtos {

    private CommentDtos() {
    }

    public record CommentRequest(@NotBlank @Size(max = 4000) String content) {
    }

    public record CommentResponse(Long id, Long taskId, UserDtos.UserSummary author, String content,
                                  Instant createdAt, Instant updatedAt) {
        public static CommentResponse from(Comment comment) {
            return new CommentResponse(comment.getId(), comment.getTask().getId(),
                    UserDtos.UserSummary.of(comment.getAuthor()), comment.getContent(),
                    comment.getCreatedAt(), comment.getUpdatedAt());
        }
    }
}
