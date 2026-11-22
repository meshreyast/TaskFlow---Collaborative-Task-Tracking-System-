package com.taskflow.service;

import com.taskflow.domain.Comment;
import com.taskflow.domain.Task;
import com.taskflow.domain.User;
import com.taskflow.dto.CommentDtos.CommentRequest;
import com.taskflow.dto.CommentDtos.CommentResponse;
import com.taskflow.exception.ApiException;
import com.taskflow.repository.CommentRepository;
import com.taskflow.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Anyone who can view a task can read and write comments on it; edits are limited to the author. */
@Service
public class CommentService {

    private final CommentRepository comments;
    private final UserRepository users;
    private final TaskAccessService taskAccess;

    public CommentService(CommentRepository comments, UserRepository users, TaskAccessService taskAccess) {
        this.comments = comments;
        this.users = users;
        this.taskAccess = taskAccess;
    }

    @Transactional
    public CommentResponse add(Long userId, Long taskId, CommentRequest request) {
        Task task = taskAccess.requireVisible(taskId, userId);
        User author = users.findById(userId)
                .orElseThrow(() -> ApiException.unauthorized("User no longer exists"));
        return CommentResponse.from(comments.save(new Comment(task, author, request.content().trim())));
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> list(Long userId, Long taskId) {
        taskAccess.requireVisible(taskId, userId);
        return comments.findAllByTaskIdOrderByCreatedAtAsc(taskId).stream()
                .map(CommentResponse::from)
                .toList();
    }

    @Transactional
    public CommentResponse update(Long userId, Long taskId, Long commentId, CommentRequest request) {
        Comment comment = find(userId, taskId, commentId);
        if (!comment.getAuthor().getId().equals(userId)) {
            throw ApiException.forbidden("Only the author can edit a comment");
        }
        comment.setContent(request.content().trim());
        return CommentResponse.from(comments.saveAndFlush(comment));
    }

    /** The author or the team owner can delete a comment. */
    @Transactional
    public void delete(Long userId, Long taskId, Long commentId) {
        Comment comment = find(userId, taskId, commentId);
        boolean isAuthor = comment.getAuthor().getId().equals(userId);
        if (!isAuthor && !taskAccess.isTeamOwner(comment.getTask(), userId)) {
            throw ApiException.forbidden("Only the author or the team owner can delete a comment");
        }
        comments.delete(comment);
    }

    private Comment find(Long userId, Long taskId, Long commentId) {
        taskAccess.requireVisible(taskId, userId);
        return comments.findById(commentId)
                .filter(c -> c.getTask().getId().equals(taskId))
                .orElseThrow(() -> ApiException.notFound("Comment not found"));
    }
}
