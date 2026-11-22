package com.taskflow.service;

import com.taskflow.domain.Task;
import com.taskflow.domain.User;
import com.taskflow.exception.ApiException;
import com.taskflow.repository.TaskRepository;
import com.taskflow.repository.TeamMemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Task authorization rules, shared by the task, comment and attachment services.
 * <ul>
 *   <li><b>View / comment / attach:</b> every member of the task's team; for a personal task, its
 *       creator or assignee.</li>
 *   <li><b>Edit / change status / assign:</b> the task's creator, its assignee, or the team owner.</li>
 *   <li><b>Delete:</b> the task's creator or the team owner.</li>
 * </ul>
 * Tasks a user may not view are reported as 404 so their existence is not revealed.
 */
@Service
@Transactional(readOnly = true)
public class TaskAccessService {

    private final TaskRepository tasks;
    private final TeamMemberRepository members;

    public TaskAccessService(TaskRepository tasks, TeamMemberRepository members) {
        this.tasks = tasks;
        this.members = members;
    }

    public Task requireVisible(Long taskId, Long userId) {
        Task task = tasks.findById(taskId).orElseThrow(() -> ApiException.notFound("Task not found"));
        boolean visible = task.getTeam() != null
                ? members.existsByTeamIdAndUserId(task.getTeam().getId(), userId)
                : isUser(task.getCreatedBy(), userId) || isUser(task.getAssignee(), userId);
        if (!visible) {
            throw ApiException.notFound("Task not found");
        }
        return task;
    }

    public Task requireManageable(Long taskId, Long userId) {
        Task task = requireVisible(taskId, userId);
        if (!canManage(task, userId)) {
            throw ApiException.forbidden("Only the creator, the assignee or the team owner can modify this task");
        }
        return task;
    }

    public boolean canManage(Task task, Long userId) {
        return isUser(task.getCreatedBy(), userId) || isUser(task.getAssignee(), userId)
                || isTeamOwner(task, userId);
    }

    public boolean canDelete(Task task, Long userId) {
        return isUser(task.getCreatedBy(), userId) || isTeamOwner(task, userId);
    }

    public boolean isTeamOwner(Task task, Long userId) {
        return task.getTeam() != null && task.getTeam().getOwner().getId().equals(userId);
    }

    private static boolean isUser(User user, Long userId) {
        return user != null && user.getId().equals(userId);
    }
}
