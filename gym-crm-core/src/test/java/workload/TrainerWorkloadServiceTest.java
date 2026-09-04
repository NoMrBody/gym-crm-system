package workload;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.ResourceAccessException;

import java.time.Duration;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TrainerWorkloadServiceTest {

    private static final int FAILURES_BEFORE_OPEN = 3;

    @Mock
    private TrainerWorkloadClient client;

    private CircuitBreakerRegistry registry;
    private TrainerWorkloadService service;

    @BeforeEach
    void setUp() {
        // Same shape as the configured breaker, only small enough to trip within a test.
        registry = CircuitBreakerRegistry.of(CircuitBreakerConfig.custom()
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(FAILURES_BEFORE_OPEN)
                .minimumNumberOfCalls(FAILURES_BEFORE_OPEN)
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofMinutes(1))
                .build());
        service = new TrainerWorkloadService(client, registry);
    }

    @Test
    void aSuccessfulCallForwardsTheRequest() {
        TrainerWorkloadRequest request = request(ActionType.ADD);

        service.notifyWorkloadChange(request);

        verify(client).submitWorkload(request);
    }

    @Test
    void aFailingCallIsSwallowedSoTheTrainingStillSucceeds() {
        doThrow(new ResourceAccessException("connection refused"))
                .when(client).submitWorkload(any());

        assertDoesNotThrow(() -> service.notifyWorkloadChange(request(ActionType.ADD)));
    }

    @Test
    void onceTheBreakerOpensTheClientIsNoLongerCalled() {
        doThrow(new ResourceAccessException("connection refused"))
                .when(client).submitWorkload(any());

        for (int attempt = 0; attempt < FAILURES_BEFORE_OPEN; attempt++) {
            service.notifyWorkloadChange(request(ActionType.ADD));
        }
        assertEquals(CircuitBreaker.State.OPEN,
                registry.circuitBreaker(TrainerWorkloadService.CIRCUIT_BREAKER).getState());

        // Further events are short-circuited rather than attempted again.
        service.notifyWorkloadChange(request(ActionType.ADD));
        service.notifyWorkloadChange(request(ActionType.DELETE));

        verify(client, times(FAILURES_BEFORE_OPEN)).submitWorkload(any());
    }

    private static TrainerWorkloadRequest request(ActionType actionType) {
        return new TrainerWorkloadRequest("Alice.Cooper", "Alice", "Cooper", true,
                LocalDate.of(2026, 3, 12), 60, actionType);
    }
}
