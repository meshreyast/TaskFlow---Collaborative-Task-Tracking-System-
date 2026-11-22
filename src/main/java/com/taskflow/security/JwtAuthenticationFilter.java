package com.taskflow.security;

import com.taskflow.repository.RevokedTokenRepository;
import com.taskflow.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Authenticates a request from its "Authorization: Bearer" token. Invalid, expired, revoked
 * (logged-out) tokens and tokens of deleted users leave the request unauthenticated, so the
 * security chain answers 401.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final RevokedTokenRepository revokedTokens;
    private final UserRepository users;

    public JwtAuthenticationFilter(JwtService jwtService, RevokedTokenRepository revokedTokens, UserRepository users) {
        this.jwtService = jwtService;
        this.revokedTokens = revokedTokens;
        this.users = users;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(PREFIX)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            Optional<JwtService.ParsedToken> parsed = jwtService.parse(header.substring(PREFIX.length()).trim());
            if (parsed.isPresent() && !revokedTokens.existsById(parsed.get().tokenId())) {
                JwtService.ParsedToken token = parsed.get();
                users.findById(token.userId()).ifPresent(user -> {
                    CurrentUser principal = new CurrentUser(user.getId(), user.getEmail(),
                            token.tokenId(), token.expiresAt());
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            principal, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                });
            }
        }
        chain.doFilter(request, response);
    }
}
