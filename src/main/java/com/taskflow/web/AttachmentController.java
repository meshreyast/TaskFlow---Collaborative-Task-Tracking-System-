package com.taskflow.web;

import com.taskflow.dto.AttachmentDtos.AttachmentResponse;
import com.taskflow.security.CurrentUser;
import com.taskflow.service.AttachmentService;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/tasks/{taskId}/attachments")
public class AttachmentController {

    private final AttachmentService attachmentService;

    public AttachmentController(AttachmentService attachmentService) {
        this.attachmentService = attachmentService;
    }

    /** multipart/form-data with a single part named "file" (max 10 MB). */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public AttachmentResponse upload(@AuthenticationPrincipal CurrentUser user, @PathVariable Long taskId,
                                     @RequestParam("file") MultipartFile file) {
        return attachmentService.upload(user.id(), taskId, file);
    }

    @GetMapping
    public List<AttachmentResponse> list(@AuthenticationPrincipal CurrentUser user, @PathVariable Long taskId) {
        return attachmentService.list(user.id(), taskId);
    }

    /** Always served as a download (never inline), so uploaded HTML or SVG cannot run in the API's origin. */
    @GetMapping("/{attachmentId}/download")
    public ResponseEntity<Resource> download(@AuthenticationPrincipal CurrentUser user, @PathVariable Long taskId,
                                             @PathVariable Long attachmentId) {
        AttachmentService.Download download = attachmentService.download(user.id(), taskId, attachmentId);
        return ResponseEntity.ok()
                .contentType(download.mediaType())
                .contentLength(download.size())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(download.fileName(), StandardCharsets.UTF_8).build().toString())
                .body(download.resource());
    }

    @DeleteMapping("/{attachmentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal CurrentUser user, @PathVariable Long taskId,
                       @PathVariable Long attachmentId) {
        attachmentService.delete(user.id(), taskId, attachmentId);
    }
}
