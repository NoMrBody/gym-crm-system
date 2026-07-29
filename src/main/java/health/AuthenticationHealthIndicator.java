package health;

import metrics.GymMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class AuthenticationHealthIndicator implements HealthIndicator {
    private static final Logger log = LoggerFactory.getLogger(AuthenticationHealthIndicator.class);

    private final GymMetrics gymMetrics;
    private final long minAttempts;
    private final double maxFailureRatio;

    public AuthenticationHealthIndicator(
            GymMetrics gymMetrics,
            @Value("${gym.health.auth.min-attempts:10}") long minAttempts,
            @Value("${gym.health.auth.max-failure-ratio:0.5}") double maxFailureRatio) {
        this.gymMetrics = gymMetrics;
        this.minAttempts = minAttempts;
        this.maxFailureRatio = maxFailureRatio;
    }

    @Override
    public Health health() {
        double successes = gymMetrics.getAuthSuccesses();
        double failures = gymMetrics.getAuthFailures();
        double total = successes + failures;

        if (total < minAttempts) {
            log.debug("Authentication health check UP: insufficient samples (total={})", total);
            return Health.up()
                    .withDetail("successes", successes)
                    .withDetail("failures", failures)
                    .withDetail("total", total)
                    .withDetail("minAttempts", minAttempts)
                    .withDetail("note", "insufficient samples")
                    .build();
        }

        double failureRatio = failures / total;
        if (failureRatio > maxFailureRatio) {
            log.warn("Authentication health check DOWN: failureRatio={} exceeds max={}",
                    failureRatio, maxFailureRatio);
            return Health.down()
                    .withDetail("successes", successes)
                    .withDetail("failures", failures)
                    .withDetail("failureRatio", failureRatio)
                    .withDetail("maxFailureRatio", maxFailureRatio)
                    .build();
        }

        log.debug("Authentication health check OK: failureRatio={}", failureRatio);
        return Health.up()
                .withDetail("successes", successes)
                .withDetail("failures", failures)
                .withDetail("failureRatio", failureRatio)
                .withDetail("maxFailureRatio", maxFailureRatio)
                .build();
    }
}
