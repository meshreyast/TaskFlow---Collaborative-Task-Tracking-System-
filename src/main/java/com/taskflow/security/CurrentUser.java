package com.taskflow.security;

import java.time.Instant;

/** The authenticated principal, built from a validated JWT. */
public record CurrentUser(Long id, String email, String tokenId, Instant tokenExpiresAt) {
}
