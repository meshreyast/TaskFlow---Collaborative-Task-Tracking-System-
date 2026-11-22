package com.taskflow.dto;

import com.taskflow.domain.Task;
import com.taskflow.domain.TaskPriority;
import com.taskflow.domain.TaskStatus;
import com.taskflow.domain.Team;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDate;

public final class TaskDtos {

    private TaskDtos() {
    }

    /**
     * teamId omitted = personal task (assigned to the creator). With a teamId the creator must be a
     * member, and assigneeId (optional) must be a member too. priority defaults to MEDIUM.
     */
    public record CreateTaskRequest(
            @NotBlank @Size(max = 200) String title,
            @Size(max = 4000) String description,
            @FutureOrPresent LocalDate dueDate,
            TaskPriority priority,
            Long teamId,
            Long assigneeId) {
    }

    /** Full update: description and dueDate are cleared when omitted. Status and assignee have own endpoints. */
    public record UpdateTaskRequest(
            @NotBlank @Size(max = 200) String title,
            @Size(max = 4000) String description,
            LocalDate dueDate,
            @NotNull TaskPriority priority) {
    }

    public record StatusRequest(@NotNull TaskStatus status) {
    }

    /** assigneeId = null unassigns the task. */
    public record AssignRequest(Long assigneeId) {
    }

    public record TaskFilter(TaskStatus status, TaskPriority priority, Long teamId, Long assigneeId,
                             boolean assignedToMe, boolean overdue, LocalDate dueFrom, LocalDate dueTo,
                             String q) {
    }

    public record TaskResponse(Long id, String title, String description, TaskStatus status,
                               TaskPriority priority, LocalDate dueDate, Long teamId, String teamName,
                               UserDtos.UserSummary createdBy, UserDtos.UserSummary assignee,
                               Instant createdAt, Instant updatedAt, Instant completedAt) {
        public static TaskResponse from(Task task) {
            Team team = task.getTeam();
            return new TaskResponse(task.getId(), task.getTitle(), task.getDescription(), task.getStatus(),
                    task.getPriority(), task.getDueDate(),
                    team == null ? null : team.getId(),
                    team == null ? null : team.getName(),
                    UserDtos.UserSummary.of(task.getCreatedBy()),
                    UserDtos.UserSummary.of(task.getAssignee()),
                    task.getCreatedAt(), task.getUpdatedAt(), task.getCompletedAt());
        }
    }
}
