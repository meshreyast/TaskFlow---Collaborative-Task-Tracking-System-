package com.taskflow.service;

import com.taskflow.domain.Attachment;
import com.taskflow.domain.Task;
import com.taskflow.domain.TaskPriority;
import com.taskflow.domain.TaskStatus;
import com.taskflow.domain.Team;
import com.taskflow.domain.User;
import com.taskflow.dto.PageResponse;
import com.taskflow.dto.TaskDtos.CreateTaskRequest;
import com.taskflow.dto.TaskDtos.TaskFilter;
import com.taskflow.dto.TaskDtos.TaskResponse;
import com.taskflow.dto.TaskDtos.UpdateTaskRequest;
import com.taskflow.exception.ApiException;
import com.taskflow.repository.AttachmentRepository;
import com.taskflow.repository.CommentRepository;
import com.taskflow.repository.TaskRepository;
import com.taskflow.repository.TeamMemberRepository;
import com.taskflow.repository.UserRepository;
import com.taskflow.util.Text;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
public class TaskService {

    /** Properties a client may sort by. Anything else is rejected with 400 instead of failing in the query. */
    static final Set<String> SORTABLE_PROPERTIES = Set.of("createdAt", "updatedAt", "dueDate", "title");

    private final TaskRepository tasks;
    private final CommentRepository comments;
    private final AttachmentRepository attachments;
    private final TeamMemberRepository members;
    private final UserRepository users;
    private final TeamAccessService teamAccess;
    private final TaskAccessService taskAccess;
    private final FileStorageService storage;

    public TaskService(TaskRepository tasks, CommentRepository comments, AttachmentRepository attachments,
                       TeamMemberRepository members, UserRepository users, TeamAccessService teamAccess,
                       TaskAccessService taskAccess, FileStorageService storage) {
        this.tasks = tasks;
        this.comments = comments;
        this.attachments = attachments;
        this.members = members;
        this.users = users;
        this.teamAccess = teamAccess;
        this.taskAccess = taskAccess;
        this.storage = storage;
    }

    @Transactional
    public TaskResponse create(Long userId, CreateTaskRequest request) {
        User creator = users.findById(userId)
                .orElseThrow(() -> ApiException.unauthorized("User no longer exists"));
        Team team = request.teamId() == null ? null : teamAccess.requireMember(request.teamId(), userId);
        User assignee = resolveAssignee(team, userId, request.assigneeId());
        if (team == null && assignee == null) {
            assignee = creator; // a personal task belongs to its creator
        }

        Task task = new Task();
        task.setTitle(request.title().trim());
        task.setDescription(Text.trimToNull(request.description()));
        task.setDueDate(request.dueDate());
        task.setPriority(request.priority() != null ? request.priority() : TaskPriority.MEDIUM);
        task.setTeam(team);
        task.setCreatedBy(creator);
        task.setAssignee(assignee);
        return TaskResponse.from(tasks.save(task));
    }

    @Transactional(readOnly = true)
    public TaskResponse get(Long userId, Long taskId) {
        return TaskResponse.from(taskAccess.requireVisible(taskId, userId));
    }

    @Transactional(readOnly = true)
    public PageResponse<TaskResponse> list(Long userId, TaskFilter filter, Pageable pageable) {
        // Appending id makes the order total, so pages never overlap or skip rows.
        Pageable page = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                validated(pageable.getSort()).and(Sort.by(Sort.Direction.ASC, "id")));

        Long assigneeId = filter.assignedToMe() ? userId : filter.assigneeId();
        Specification<Task> spec = TaskSpecifications.visibleTo(userId, members.findTeamIdsByUserId(userId))
                .and(TaskSpecifications.hasStatus(filter.status()))
                .and(TaskSpecifications.hasPriority(filter.priority()))
                .and(TaskSpecifications.inTeam(filter.teamId()))
                .and(TaskSpecifications.assignedTo(assigneeId))
                .and(TaskSpecifications.dueOnOrAfter(filter.dueFrom()))
                .and(TaskSpecifications.dueOnOrBefore(filter.dueTo()))
                .and(TaskSpecifications.overdue(filter.overdue()))
                .and(TaskSpecifications.matchesText(filter.q()));

        return PageResponse.of(tasks.findAll(spec, page), TaskResponse::from);
    }

    @Transactional
    public TaskResponse update(Long userId, Long taskId, UpdateTaskRequest request) {
        Task task = taskAccess.requireManageable(taskId, userId);
        task.setTitle(request.title().trim());
        task.setDescription(Text.trimToNull(request.description()));
        task.setDueDate(request.dueDate());
        task.setPriority(request.priority());
        return TaskResponse.from(tasks.saveAndFlush(task));
    }

    @Transactional
    public TaskResponse changeStatus(Long userId, Long taskId, TaskStatus status) {
        Task task = taskAccess.requireManageable(taskId, userId);
        task.changeStatus(status);
        return TaskResponse.from(tasks.saveAndFlush(task));
    }

    /** Assigns the task to a team member, or unassigns it when assigneeId is null. */
    @Transactional
    public TaskResponse assign(Long userId, Long taskId, Long assigneeId) {
        Task task = taskAccess.requireManageable(taskId, userId);
        task.setAssignee(resolveAssignee(task.getTeam(), userId, assigneeId));
        return TaskResponse.from(tasks.saveAndFlush(task));
    }

    @Transactional
    public void delete(Long userId, Long taskId) {
        Task task = taskAccess.requireVisible(taskId, userId);
        if (!taskAccess.canDelete(task, userId)) {
            throw ApiException.forbidden("Only the creator or the team owner can delete this task");
        }
        deleteWithDependents(task);
    }

    /** Used when a team is deleted; the caller has already checked permissions. */
    @Transactional
    public void deleteAllForTeam(Long teamId) {
        tasks.findAllByTeamId(teamId).forEach(this::deleteWithDependents);
    }

    private void deleteWithDependents(Task task) {
        List<String> storageKeys = attachments.findAllByTaskId(task.getId()).stream()
                .map(Attachment::getStorageKey)
                .toList();
        storage.deleteAfterCommit(storageKeys);
        attachments.deleteAllByTaskId(task.getId());
        comments.deleteAllByTaskId(task.getId());
        tasks.delete(task);
    }

    /**
     * Team tasks can only be assigned to members of that team. Personal tasks can only be
     * assigned to the user themselves.
     */
    private User resolveAssignee(Team team, Long actorId, Long assigneeId) {
        if (assigneeId == null) {
            return null;
        }
        if (team == null) {
            if (!assigneeId.equals(actorId)) {
                throw ApiException.badRequest(
                        "Personal tasks can only be assigned to yourself; create the task in a team to assign it to others");
            }
        } else if (!members.existsByTeamIdAndUserId(team.getId(), assigneeId)) {
            throw ApiException.badRequest("Assignee must be a member of the team");
        }
        return users.getReferenceById(assigneeId);
    }

    private static Sort validated(Sort sort) {
        sort.forEach(order -> {
            if (!SORTABLE_PROPERTIES.contains(order.getProperty())) {
                throw ApiException.badRequest("Cannot sort by '" + order.getProperty()
                        + "'. Allowed: " + String.join(", ", SORTABLE_PROPERTIES.stream().sorted().toList()));
            }
        });
        return sort;
    }
}
