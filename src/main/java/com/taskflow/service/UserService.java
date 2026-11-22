package com.taskflow.service;

import com.taskflow.domain.User;
import com.taskflow.dto.UserDtos.ChangePasswordRequest;
import com.taskflow.dto.UserDtos.UpdateProfileRequest;
import com.taskflow.dto.UserDtos.UserResponse;
import com.taskflow.exception.ApiException;
import com.taskflow.repository.UserRepository;
import com.taskflow.util.Text;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository users;
    private final PasswordEncoder encoder;

    public UserService(UserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    @Transactional(readOnly = true)
    public UserResponse getProfile(Long userId) {
        return UserResponse.from(load(userId));
    }

    @Transactional
    public UserResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = load(userId);
        String email = Text.normalizeEmail(request.email());
        if (!email.equals(user.getEmail()) && users.existsByEmail(email)) {
            throw ApiException.conflict("Email is already registered");
        }
        user.setName(request.name().trim());
        user.setEmail(email);
        return UserResponse.from(user);
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = load(userId);
        if (!encoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw ApiException.badRequest("Current password is incorrect");
        }
        user.setPasswordHash(encoder.encode(request.newPassword()));
    }

    private User load(Long userId) {
        return users.findById(userId).orElseThrow(() -> ApiException.unauthorized("User no longer exists"));
    }
}
