package com.taskflow.service;

import com.taskflow.repository.RevokedTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/** Removes revoked-token rows whose tokens have expired anyway, keeping the table small. */
@Component
public class TokenCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(TokenCleanupJob.class);

    private final RevokedTokenRepository revokedTokens;

    public TokenCleanupJob(RevokedTokenRepository revokedTokens) {
        this.revokedTokens = revokedTokens;
    }

    @Scheduled(initialDelayString = "PT5M", fixedDelayString = "PT1H")
    @Transactional
    public void purgeExpired() {
        int removed = revokedTokens.deleteExpired(Instant.now());
        if (removed > 0) {
            log.info("Purged {} expired revoked tokens", removed);
        }
    }
}
