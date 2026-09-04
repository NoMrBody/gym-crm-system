package workload;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Reports training changes to trainer-workload-service behind a circuit breaker.
 *
 * <p>Workload reporting is secondary to the training itself, so nothing here propagates: a
 * workload service that is down or slow must not fail the request that already committed.
 */
@Service
public class TrainerWorkloadService {
    private static final Logger log = LoggerFactory.getLogger(TrainerWorkloadService.class);

    /** Matches {@code resilience4j.circuitbreaker.instances.trainerWorkload} in the properties. */
    public static final String CIRCUIT_BREAKER = "trainerWorkload";

    private final TrainerWorkloadClient client;
    private final CircuitBreakerRegistry circuitBreakerRegistry;

    public TrainerWorkloadService(TrainerWorkloadClient client, CircuitBreakerRegistry circuitBreakerRegistry) {
        this.client = client;
        this.circuitBreakerRegistry = circuitBreakerRegistry;
    }

    public void notifyWorkloadChange(TrainerWorkloadRequest request) {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(CIRCUIT_BREAKER);
        try {
            circuitBreaker.executeRunnable(() -> client.submitWorkload(request));
            log.info("Reported {} of {} minutes for trainer '{}' on {}",
                    request.actionType(), request.trainingDuration(),
                    request.trainerUsername(), request.trainingDate());
        } catch (CallNotPermittedException ex) {
            log.warn("Circuit '{}' is open, workload update skipped: {} for trainer '{}' on {}",
                    CIRCUIT_BREAKER, request.actionType(),
                    request.trainerUsername(), request.trainingDate());
        } catch (Exception ex) {
            log.error("Failed to report {} for trainer '{}' on {}: {}",
                    request.actionType(), request.trainerUsername(), request.trainingDate(), ex.getMessage());
        }
    }
}
