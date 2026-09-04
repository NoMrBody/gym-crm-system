package security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory registry of revoked JWT ids. Because tokens are stateless, logout can only be
 * enforced by remembering the revoked ids until they would have expired anyway.
 */
@Component
public class TokenDenylist {
    private static final Logger log = LoggerFactory.getLogger(TokenDenylist.class);

    private final Map<String, Instant> revoked = new ConcurrentHashMap<>();

    public void revoke(String tokenId, Instant expiresAt) {
        if (tokenId == null) {
            return;
        }
        revoked.put(tokenId, expiresAt != null ? expiresAt : Instant.now());
        log.debug("Revoked token id {} (expires at {})", tokenId, expiresAt);
    }

    public boolean isRevoked(String tokenId) {
        if (tokenId == null) {
            return false;
        }
        Instant expiresAt = revoked.get(tokenId);
        if (expiresAt == null) {
            return false;
        }
        if (expiresAt.isBefore(Instant.now())) {
            revoked.remove(tokenId);
            return false;
        }
        return true;
    }

    public int size() {
        return revoked.size();
    }

    @Scheduled(fixedDelayString = "${gym.security.jwt.denylist-cleanup-ms:300000}")
    public void purgeExpired() {
        Instant now = Instant.now();
        int before = revoked.size();
        revoked.entrySet().removeIf(entry -> entry.getValue().isBefore(now));
        int purged = before - revoked.size();
        if (purged > 0) {
            log.debug("Purged {} expired token id(s) from the denylist, {} remaining", purged, revoked.size());
        }
    }
}
