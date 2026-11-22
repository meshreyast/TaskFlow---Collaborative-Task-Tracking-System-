package com.taskflow.web;

import com.taskflow.domain.TaskPriority;
import com.taskflow.domain.TaskStatus;
import com.taskflow.dto.PageResponse;
import com.taskflow.dto.TaskDtos.AssignRequest;
import com.taskflow.dto.TaskDtos.CreateTaskRequest;
import com.taskflow.dto.TaskDtos.StatusRequest;
import com.taskflow.dto.TaskDtos.TaskFilter;
import com.taskflow.dto.TaskDtos.TaskResponse;
import com.taskflow.dto.TaskDtos.UpdateTaskRequest;
import com.taskflow.security.CurrentUser;
import com.taskflow.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TaskResponse create(@AuthenticationPrincipal CurrentUser user,
                               @Valid @RequestBody CreateTaskRequest request) {
        return taskService.create(user.id(), request);
    }

    /**
     * Lists the tasks the user can see (their teams' tasks and their personal tasks), filtered, searched
     * and sorted. Sorting: {@code sort=dueDate,asc}; allowed properties are createdAt, updatedAt, dueDate
     * and title. Dates are ISO (yyyy-MM-dd) and dueFrom/dueTo are inclusive.
     */
    @GetMapping
    public PageResponse<TaskResponse> list(
            @AuthenticationPrincipal CurrentUser user,
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) TaskPriority priority,
            @RequestParam(required = false) Long teamId,
            @RequestParam(required = false) Long assigneeId,
            @RequestParam(defaultValue = "false") boolean assignedToMe,
            @RequestParam(defaultValue = "false") boolean overdue,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueTo,
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        TaskFilter filter = new TaskFilter(status, priority, teamId, assigneeId, assignedToMe, overdue,
                dueFrom, dueTo, q);
        return taskService.list(user.id(), filter, pageable);
    }

    @GetMapping("/{taskId}")
    public TaskResponse get(@AuthenticationPrincipal CurrentUser user, @PathVariable Long taskId) {
        return taskService.get(user.id(), taskId);
    }

    @PutMapping("/{taskId}")
    public TaskResponse update(@AuthenticationPrincipal CurrentUser user, @PathVariable Long taskId,
                               @Valid @RequestBody UpdateTaskRequest request) {
        return taskService.update(user.id(), taskId, request);
    }

    /** Mark a task as OPEN, IN_PROGRESS or COMPLETED. */
    @PatchMapping("/{taskId}/status")
    public TaskResponse changeStatus(@AuthenticationPrincipal CurrentUser user, @PathVariable Long taskId,
                                     @Valid @RequestBody StatusRequest request) {
        return taskService.changeStatus(user.id(), taskId, request.status());
    }

    /** Assign to a team member; {@code {"assigneeId": null}} unassigns. */
    @PutMapping("/{taskId}/assignee")
    public TaskResponse assign(@AuthenticationPrincipal CurrentUser user, @PathVariable Long taskId,
                               @RequestBody AssignRequest request) {
        return taskService.assign(user.id(), taskId, request.assigneeId());
    }

    @DeleteMapping("/{taskId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal CurrentUser user, @PathVariable Long taskId) {
        taskService.delete(user.id(), taskId);
    }
}
