package com.taskflow.dto;

import com.taskflow.domain.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class UserDtos {

    private UserDtos() {
    }

    public record UserResponse(Long id, String name, String email, Instant createdAt) {
        public static UserResponse from(User user) {
            return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getCreatedAt());
        }
    }

    /** Compact user reference embedded in tasks, comments, team members, etc. */
    public record UserSummary(Long id, String name, String email) {
        public static UserSummary of(User user) {
            return user == null ? null : new UserSummary(user.getId(), user.getName(), user.getEmail());
        }
    }

    public record UpdateProfileRequest(
            @NotBlank @Size(max = 100) String name,
            @NotBlank @Email @Size(max = 255) String email) {
    }

    public record ChangePasswordRequest(
            @NotBlank String currentPassword,
            @NotBlank @Size(min = 8, max = 72, message = "must be between 8 and 72 characters") String newPassword) {
    }
}
