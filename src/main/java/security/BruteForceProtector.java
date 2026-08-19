package security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks consecutive failed logins per username and locks the account for a cool-down
 * period once the threshold is reached. State is in-memory, so it resets on restart.
 */
@Component
public class BruteForceProtector {
    private static final Logger log = LoggerFactory.getLogger(BruteForceProtector.class);

    private final Map<String, Attempts> attemptsByUser = new ConcurrentHashMap<>();
    private final int maxAttempts;
    private final Duration blockDuration;

    public BruteForceProtector(@Value("${gym.security.brute-force.max-attempts}") int maxAttempts,
                               @Value("${gym.security.brute-force.block-duration}") Duration blockDuration) {
        this.maxAttempts = maxAttempts;
        this.blockDuration = blockDuration;
    }

    public boolean isBlocked(String username) {
        return blockedUntil(username) != null;
    }

    /**
     * Returns the moment the block lifts, or {@code null} when the user is not blocked.
     * An elapsed block is cleared as a side effect so the next attempt starts fresh.
     */
    public Instant blockedUntil(String username) {
        Attempts attempts = attemptsByUser.get(key(username));
        if (attempts == null || attempts.blockedUntil() == null) {
            return null;
        }
        if (!attempts.blockedUntil().isAfter(Instant.now())) {
            reset(username);
            return null;
        }
        return attempts.blockedUntil();
    }

    public void recordFailure(String username) {
        String key = key(username);
        Attempts updated = attemptsByUser.compute(key, (ignored, current) -> {
            Instant now = Instant.now();
            int failures = (current == null ? 0 : current.failures()) + 1;
            Instant blockedUntil = failures >= maxAttempts ? now.plus(blockDuration) : null;
            return new Attempts(failures, blockedUntil, now);
        });

        if (updated.blockedUntil() != null) {
            log.warn("User '{}' blocked until {} after {} consecutive failed login attempts",
                    username, updated.blockedUntil(), updated.failures());
        } else {
            log.debug("Failed login {} of {} for user '{}'", updated.failures(), maxAttempts, username);
        }
    }

    public void reset(String username) {
        if (attemptsByUser.remove(key(username)) != null) {
            log.debug("Cleared failed login attempts for user '{}'", username);
        }
    }

    public Duration remainingBlock(String username) {
        Instant until = blockedUntil(username);
        return until == null ? Duration.ZERO : Duration.between(Instant.now(), until);
    }

    /**
     * Drops lifted blocks and counters that have gone quiet, so that probing with random
     * usernames cannot grow the map without bound.
     */
    @Scheduled(fixedDelayString = "${gym.security.brute-force.cleanup-ms:300000}")
    public void purgeExpired() {
        Instant now = Instant.now();
        Instant staleBefore = now.minus(blockDuration);
        int before = attemptsByUser.size();
        attemptsByUser.entrySet().removeIf(entry -> {
            Attempts attempts = entry.getValue();
            return attempts.blockedUntil() != null
                    ? !attempts.blockedUntil().isAfter(now)
                    : attempts.lastFailureAt().isBefore(staleBefore);
        });
        int purged = before - attemptsByUser.size();
        if (purged > 0) {
            log.debug("Purged {} stale login attempt record(s), {} entries remaining", purged, attemptsByUser.size());
        }
    }

    private static String key(String username) {
        return username == null ? "" : username.toLowerCase(Locale.ROOT);
    }

    private record Attempts(int failures, Instant blockedUntil, Instant lastFailureAt) {
    }
}
