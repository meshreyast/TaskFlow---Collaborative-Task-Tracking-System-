package com.taskflow.dto;

import com.taskflow.domain.Attachment;

import java.time.Instant;

public final class AttachmentDtos {

    private AttachmentDtos() {
    }

    public record AttachmentResponse(Long id, Long taskId, String fileName, String contentType, long sizeBytes,
                                     UserDtos.UserSummary uploadedBy, Instant createdAt) {
        public static AttachmentResponse from(Attachment attachment) {
            return new AttachmentResponse(attachment.getId(), attachment.getTask().getId(),
                    attachment.getFileName(), attachment.getContentType(), attachment.getSizeBytes(),
                    UserDtos.UserSummary.of(attachment.getUploadedBy()), attachment.getCreatedAt());
        }
    }
}
