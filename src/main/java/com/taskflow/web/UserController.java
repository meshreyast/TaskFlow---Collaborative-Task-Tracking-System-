package com.taskflow.web;

import com.taskflow.dto.UserDtos.ChangePasswordRequest;
import com.taskflow.dto.UserDtos.UpdateProfileRequest;
import com.taskflow.dto.UserDtos.UserResponse;
import com.taskflow.security.CurrentUser;
import com.taskflow.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/me")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public UserResponse me(@AuthenticationPrincipal CurrentUser user) {
        return userService.getProfile(user.id());
    }

    @PutMapping
    public UserResponse update(@AuthenticationPrincipal CurrentUser user,
                               @Valid @RequestBody UpdateProfileRequest request) {
        return userService.updateProfile(user.id(), request);
    }

    @PutMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@AuthenticationPrincipal CurrentUser user,
                               @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(user.id(), request);
    }
}
