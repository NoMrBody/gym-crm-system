package workload;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Publishes training changes to trainer-workload-service over ActiveMQ.
 * Workload reporting is secondary to the training itself, so nothing here propagates: a
 * broker that is down or slow must not fail the request that already committed.
 */
@Service
public class TrainerWorkloadService {
    private static final Logger log = LoggerFactory.getLogger(TrainerWorkloadService.class);

    /** Matches {@code resilience4j.circuitbreaker.instances.trainerWorkload} in the properties. */
    public static final String CIRCUIT_BREAKER = "trainerWorkload";

    private final JmsTemplate jmsTemplate;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final String queue;

    public TrainerWorkloadService(JmsTemplate jmsTemplate,
                                  CircuitBreakerRegistry circuitBreakerRegistry,
                                  @Value("${gym.workload.queue}") String queue) {
        this.jmsTemplate = jmsTemplate;
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.queue = queue;
    }

    public void notifyWorkloadChange(TrainerWorkloadRequest request) {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(CIRCUIT_BREAKER);
        try {
            circuitBreaker.executeRunnable(() -> jmsTemplate.convertAndSend(queue, request, message -> {
                String transactionId = MDC.get(JmsConfig.TRANSACTION_ID_PROPERTY);
                if (StringUtils.hasText(transactionId)) {
                    message.setStringProperty(JmsConfig.TRANSACTION_ID_PROPERTY, transactionId);
                }
                return message;
            }));
            log.info("Queued {} of {} minutes for trainer '{}' on {}",
                    request.actionType(), request.trainingDuration(),
                    request.trainerUsername(), request.trainingDate());
        } catch (CallNotPermittedException ex) {
            log.warn("Circuit '{}' is open, workload update skipped: {} for trainer '{}' on {}",
                    CIRCUIT_BREAKER, request.actionType(),
                    request.trainerUsername(), request.trainingDate());
        } catch (Exception ex) {
            log.error("Failed to queue {} for trainer '{}' on {}: {}",
                    request.actionType(), request.trainerUsername(), request.trainingDate(), ex.getMessage());
        }
    }
}
