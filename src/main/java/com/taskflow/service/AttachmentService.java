package com.taskflow.service;

import com.taskflow.domain.Attachment;
import com.taskflow.domain.Task;
import com.taskflow.domain.User;
import com.taskflow.dto.AttachmentDtos.AttachmentResponse;
import com.taskflow.exception.ApiException;
import com.taskflow.repository.AttachmentRepository;
import com.taskflow.repository.UserRepository;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/** Anyone who can view a task can attach and download files; deletion is limited to the uploader and managers. */
@Service
public class AttachmentService {

    private static final int MAX_FILE_NAME_LENGTH = 255;

    public record Download(Resource resource, String fileName, MediaType mediaType, long size) {
    }

    private final AttachmentRepository attachments;
    private final UserRepository users;
    private final TaskAccessService taskAccess;
    private final FileStorageService storage;

    public AttachmentService(AttachmentRepository attachments, UserRepository users,
                             TaskAccessService taskAccess, FileStorageService storage) {
        this.attachments = attachments;
        this.users = users;
        this.taskAccess = taskAccess;
        this.storage = storage;
    }

    @Transactional
    public AttachmentResponse upload(Long userId, Long taskId, MultipartFile file) {
        Task task = taskAccess.requireVisible(taskId, userId);
        if (file == null || file.isEmpty()) {
            throw ApiException.badRequest("The uploaded file is empty");
        }
        User uploader = users.findById(userId)
                .orElseThrow(() -> ApiException.unauthorized("User no longer exists"));

        String storageKey = storage.store(taskId, file);
        try {
            Attachment attachment = new Attachment(task, uploader, cleanFileName(file.getOriginalFilename()),
                    file.getContentType(), file.getSize(), storageKey);
            return AttachmentResponse.from(attachments.saveAndFlush(attachment));
        } catch (RuntimeException e) {
            storage.deleteQuietly(storageKey);
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public List<AttachmentResponse> list(Long userId, Long taskId) {
        taskAccess.requireVisible(taskId, userId);
        return attachments.findAllByTaskIdOrderByCreatedAtAsc(taskId).stream()
                .map(AttachmentResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public Download download(Long userId, Long taskId, Long attachmentId) {
        taskAccess.requireVisible(taskId, userId);
        Attachment attachment = find(taskId, attachmentId);
        Resource resource = storage.load(attachment.getStorageKey());
        if (!resource.exists()) {
            throw ApiException.notFound("The stored file is missing");
        }
        return new Download(resource, attachment.getFileName(), mediaType(attachment.getContentType()),
                attachment.getSizeBytes());
    }

    @Transactional
    public void delete(Long userId, Long taskId, Long attachmentId) {
        Task task = taskAccess.requireVisible(taskId, userId);
        Attachment attachment = find(taskId, attachmentId);
        boolean isUploader = attachment.getUploadedBy().getId().equals(userId);
        if (!isUploader && !taskAccess.canDelete(task, userId)) {
            throw ApiException.forbidden("Only the uploader, the task creator or the team owner can delete this file");
        }
        attachments.delete(attachment);
        storage.deleteAfterCommit(List.of(attachment.getStorageKey()));
    }

    private Attachment find(Long taskId, Long attachmentId) {
        return attachments.findById(attachmentId)
                .filter(a -> a.getTask().getId().equals(taskId))
                .orElseThrow(() -> ApiException.notFound("Attachment not found"));
    }

    /** Keeps only the base name and drops control characters; the name is never used as a path. */
    private static String cleanFileName(String original) {
        String name = original == null ? "" : StringUtils.cleanPath(original);
        name = name.substring(name.lastIndexOf('/') + 1).replaceAll("\\p{Cntrl}", "_").trim();
        if (name.isEmpty() || name.equals("..")) {
            name = "file";
        }
        return name.length() > MAX_FILE_NAME_LENGTH ? name.substring(0, MAX_FILE_NAME_LENGTH) : name;
    }

    private static MediaType mediaType(String contentType) {
        try {
            return contentType == null ? MediaType.APPLICATION_OCTET_STREAM : MediaType.parseMediaType(contentType);
        } catch (RuntimeException e) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}
