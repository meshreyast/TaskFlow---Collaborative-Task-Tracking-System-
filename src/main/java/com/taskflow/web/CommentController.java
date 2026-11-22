package com.taskflow.web;

import com.taskflow.dto.CommentDtos.CommentRequest;
import com.taskflow.dto.CommentDtos.CommentResponse;
import com.taskflow.security.CurrentUser;
import com.taskflow.service.CommentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tasks/{taskId}/comments")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse add(@AuthenticationPrincipal CurrentUser user, @PathVariable Long taskId,
                               @Valid @RequestBody CommentRequest request) {
        return commentService.add(user.id(), taskId, request);
    }

    @GetMapping
    public List<CommentResponse> list(@AuthenticationPrincipal CurrentUser user, @PathVariable Long taskId) {
        return commentService.list(user.id(), taskId);
    }

    @PutMapping("/{commentId}")
    public CommentResponse update(@AuthenticationPrincipal CurrentUser user, @PathVariable Long taskId,
                                  @PathVariable Long commentId, @Valid @RequestBody CommentRequest request) {
        return commentService.update(user.id(), taskId, commentId, request);
    }

    @DeleteMapping("/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal CurrentUser user, @PathVariable Long taskId,
                       @PathVariable Long commentId) {
        commentService.delete(user.id(), taskId, commentId);
    }
}
