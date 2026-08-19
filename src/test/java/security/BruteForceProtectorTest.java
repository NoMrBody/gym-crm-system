package security;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BruteForceProtectorTest {

    private static final Duration FIVE_MINUTES = Duration.ofMinutes(5);

    private static BruteForceProtector protector(Duration blockDuration) {
        return new BruteForceProtector(3, blockDuration);
    }

    @Test
    void notBlocked_beforeAnyFailure() {
        assertFalse(protector(FIVE_MINUTES).isBlocked("Jane.Smith"));
    }

    @Test
    void notBlocked_afterTwoFailures() {
        BruteForceProtector protector = protector(FIVE_MINUTES);

        protector.recordFailure("Jane.Smith");
        protector.recordFailure("Jane.Smith");

        assertFalse(protector.isBlocked("Jane.Smith"));
    }

    @Test
    void blocked_onThirdFailure() {
        BruteForceProtector protector = protector(FIVE_MINUTES);

        protector.recordFailure("Jane.Smith");
        protector.recordFailure("Jane.Smith");
        protector.recordFailure("Jane.Smith");

        assertTrue(protector.isBlocked("Jane.Smith"));
    }

    @Test
    void blockLiftsOnceTheDurationElapses() {
        BruteForceProtector protector = protector(Duration.ZERO);

        protector.recordFailure("Jane.Smith");
        protector.recordFailure("Jane.Smith");
        protector.recordFailure("Jane.Smith");

        assertFalse(protector.isBlocked("Jane.Smith"));
    }

    @Test
    void successfulLoginResetsTheCounter() {
        BruteForceProtector protector = protector(FIVE_MINUTES);
        protector.recordFailure("Jane.Smith");
        protector.recordFailure("Jane.Smith");

        protector.reset("Jane.Smith");
        protector.recordFailure("Jane.Smith");

        assertFalse(protector.isBlocked("Jane.Smith"));
    }

    @Test
    void countersAreTrackedPerUser() {
        BruteForceProtector protector = protector(FIVE_MINUTES);

        protector.recordFailure("Jane.Smith");
        protector.recordFailure("Jane.Smith");
        protector.recordFailure("Alice.Cooper");

        assertFalse(protector.isBlocked("Jane.Smith"));
        assertFalse(protector.isBlocked("Alice.Cooper"));
    }

    @Test
    void usernameMatchingIgnoresCase() {
        BruteForceProtector protector = protector(FIVE_MINUTES);

        protector.recordFailure("Jane.Smith");
        protector.recordFailure("jane.smith");
        protector.recordFailure("JANE.SMITH");

        assertTrue(protector.isBlocked("Jane.Smith"));
    }

    @Test
    void remainingBlockCountsDownWithinTheBlockWindow() {
        BruteForceProtector protector = protector(FIVE_MINUTES);
        protector.recordFailure("Jane.Smith");
        protector.recordFailure("Jane.Smith");
        protector.recordFailure("Jane.Smith");

        Duration remaining = protector.remainingBlock("Jane.Smith");

        assertTrue(remaining.compareTo(Duration.ofMinutes(4)) > 0);
        assertTrue(remaining.compareTo(FIVE_MINUTES) <= 0);
    }

    @Test
    void remainingBlockIsZeroWhenNotBlocked() {
        assertTrue(protector(FIVE_MINUTES).remainingBlock("Jane.Smith").isZero());
    }

    @Test
    void purgeDropsCountersThatHaveGoneQuiet() {
        BruteForceProtector protector = protector(Duration.ZERO);
        protector.recordFailure("Jane.Smith");

        protector.purgeExpired();

        // A fresh counter means the earlier failure was forgotten.
        protector.recordFailure("Jane.Smith");
        protector.recordFailure("Jane.Smith");
        assertFalse(protector.isBlocked("Jane.Smith"));
    }
}
