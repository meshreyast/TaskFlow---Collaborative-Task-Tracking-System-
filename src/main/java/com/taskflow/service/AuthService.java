package com.taskflow.service;

import com.taskflow.domain.RevokedToken;
import com.taskflow.domain.User;
import com.taskflow.dto.AuthDtos.AuthResponse;
import com.taskflow.dto.AuthDtos.LoginRequest;
import com.taskflow.dto.AuthDtos.RegisterRequest;
import com.taskflow.dto.UserDtos.UserResponse;
import com.taskflow.exception.ApiException;
import com.taskflow.repository.RevokedTokenRepository;
import com.taskflow.repository.UserRepository;
import com.taskflow.security.CurrentUser;
import com.taskflow.security.JwtService;
import com.taskflow.util.Text;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class AuthService {

    private final UserRepository users;
    private final RevokedTokenRepository revokedTokens;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    /** Compared against when the email is unknown so that response time does not reveal which emails exist. */
    private final String timingEqualizerHash;

    public AuthService(UserRepository users, RevokedTokenRepository revokedTokens,
                       PasswordEncoder encoder, JwtService jwt) {
        this.users = users;
        this.revokedTokens = revokedTokens;
        this.encoder = encoder;
        this.jwt = jwt;
        this.timingEqualizerHash = encoder.encode("timing-equalizer");
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = Text.normalizeEmail(request.email());
        if (users.existsByEmail(email)) {
            throw ApiException.conflict("Email is already registered");
        }
        User user = users.save(new User(request.name().trim(), email, encoder.encode(request.password())));
        return toResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        Optional<User> user = users.findByEmail(Text.normalizeEmail(request.email()));
        boolean passwordMatches = encoder.matches(request.password(),
                user.map(User::getPasswordHash).orElse(timingEqualizerHash));
        if (user.isEmpty() || !passwordMatches) {
            throw ApiException.unauthorized("Invalid email or password");
        }
        return toResponse(user.get());
    }

    /** Revokes the token used for this request so it cannot be used again, even before it expires. */
    @Transactional
    public void logout(CurrentUser current) {
        if (!revokedTokens.existsById(current.tokenId())) {
            revokedTokens.save(new RevokedToken(current.tokenId(), current.tokenExpiresAt()));
        }
    }

    private AuthResponse toResponse(User user) {
        return new AuthResponse(jwt.generate(user), "Bearer", jwt.ttlSeconds(), UserResponse.from(user));
    }
}
