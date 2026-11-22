package com.taskflow.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record RegisterRequest(
            @NotBlank @Size(max = 100) String name,
            @NotBlank @Email @Size(max = 255) String email,
            // BCrypt only uses the first 72 bytes, so longer passwords are rejected rather than truncated.
            @NotBlank @Size(min = 8, max = 72, message = "must be between 8 and 72 characters") String password) {
    }

    public record LoginRequest(@NotBlank String email, @NotBlank String password) {
    }

    public record AuthResponse(String accessToken, String tokenType, long expiresInSeconds,
                               UserDtos.UserResponse user) {
    }
}
