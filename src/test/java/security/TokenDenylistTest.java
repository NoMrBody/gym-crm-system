package security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TokenDenylistTest {

    private TokenDenylist denylist;

    @BeforeEach
    void setUp() {
        denylist = new TokenDenylist();
    }

    @Test
    void unknownTokenIsNotRevoked() {
        assertFalse(denylist.isRevoked("some-id"));
    }

    @Test
    void nullTokenIdIsNotRevoked() {
        assertFalse(denylist.isRevoked(null));
    }

    @Test
    void revokedTokenIsRejectedUntilItExpires() {
        denylist.revoke("token-id", Instant.now().plus(1, ChronoUnit.HOURS));

        assertTrue(denylist.isRevoked("token-id"));
    }

    @Test
    void revocationIsForgottenOnceTheTokenWouldHaveExpired() {
        denylist.revoke("token-id", Instant.now().minusSeconds(1));

        assertFalse(denylist.isRevoked("token-id"));
        assertEquals(0, denylist.size());
    }

    @Test
    void revokingNullTokenIdIsIgnored() {
        denylist.revoke(null, Instant.now().plusSeconds(60));

        assertEquals(0, denylist.size());
    }

    @Test
    void purgeRemovesOnlyExpiredEntries() {
        denylist.revoke("expired", Instant.now().minusSeconds(1));
        denylist.revoke("live", Instant.now().plus(1, ChronoUnit.HOURS));

        denylist.purgeExpired();

        assertEquals(1, denylist.size());
        assertTrue(denylist.isRevoked("live"));
    }
}
