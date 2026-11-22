package com.taskflow.service;

import com.taskflow.domain.Task;
import com.taskflow.domain.TaskPriority;
import com.taskflow.domain.TaskStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Locale;

/** Building blocks for the task list query. A filter that is not set returns null (= no restriction). */
final class TaskSpecifications {

    private TaskSpecifications() {
    }

    /** Tasks in any of the user's teams, plus the user's own personal tasks. */
    static Specification<Task> visibleTo(Long userId, Collection<Long> teamIds) {
        return (root, query, cb) -> {
            Predicate personal = cb.and(
                    cb.isNull(root.get("team")),
                    cb.or(cb.equal(root.get("createdBy").get("id"), userId),
                            cb.equal(root.get("assignee").get("id"), userId)));
            if (teamIds.isEmpty()) {
                return personal;
            }
            return cb.or(personal, root.get("team").get("id").in(teamIds));
        };
    }

    static Specification<Task> hasStatus(TaskStatus status) {
        if (status == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    static Specification<Task> hasPriority(TaskPriority priority) {
        if (priority == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("priority"), priority);
    }

    static Specification<Task> inTeam(Long teamId) {
        if (teamId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("team").get("id"), teamId);
    }

    static Specification<Task> assignedTo(Long userId) {
        if (userId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("assignee").get("id"), userId);
    }

    static Specification<Task> dueOnOrAfter(LocalDate date) {
        if (date == null) {
            return null;
        }
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.<LocalDate>get("dueDate"), date);
    }

    static Specification<Task> dueOnOrBefore(LocalDate date) {
        if (date == null) {
            return null;
        }
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.<LocalDate>get("dueDate"), date);
    }

    /** Past its due date and not completed. */
    static Specification<Task> overdue(boolean enabled) {
        if (!enabled) {
            return null;
        }
        LocalDate today = LocalDate.now();
        return (root, query, cb) -> cb.and(
                cb.lessThan(root.<LocalDate>get("dueDate"), today),
                cb.notEqual(root.get("status"), TaskStatus.COMPLETED));
    }

    /** Case-insensitive "contains" search over title and description. */
    static Specification<Task> matchesText(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String escaped = text.trim().toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        String pattern = "%" + escaped + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.<String>get("title")), pattern, '\\'),
                cb.like(cb.lower(root.<String>get("description")), pattern, '\\'));
    }
}
