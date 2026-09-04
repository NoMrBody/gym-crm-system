package health;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import metrics.GymMetrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.health.contributor.Status;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuthenticationHealthIndicatorTest {

    private GymMetrics gymMetrics;
    private AuthenticationHealthIndicator indicator;

    @BeforeEach
    void setUp() {
        gymMetrics = new GymMetrics(new SimpleMeterRegistry());
        indicator = new AuthenticationHealthIndicator(gymMetrics, 10, 0.5);
    }

    @Test
    void health_whenBelowMinAttempts_returnsUpEvenIfAllFailed() {
        gymMetrics.recordAuthResult(false);
        gymMetrics.recordAuthResult(false);

        var health = indicator.health();

        assertEquals(Status.UP, health.getStatus());
        assertEquals("insufficient samples", health.getDetails().get("note"));
    }

    @Test
    void health_whenFailureRatioAboveThreshold_returnsDown() {
        for (int i = 0; i < 3; i++) {
            gymMetrics.recordAuthResult(true);
        }
        for (int i = 0; i < 8; i++) {
            gymMetrics.recordAuthResult(false);
        }

        var health = indicator.health();

        assertEquals(Status.DOWN, health.getStatus());
    }

    @Test
    void health_whenFailureRatioWithinThreshold_returnsUp() {
        for (int i = 0; i < 8; i++) {
            gymMetrics.recordAuthResult(true);
        }
        for (int i = 0; i < 2; i++) {
            gymMetrics.recordAuthResult(false);
        }

        var health = indicator.health();

        assertEquals(Status.UP, health.getStatus());
    }
}
