package metrics;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import model.Training;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class GymMetricsTest {

    private SimpleMeterRegistry registry;
    private GymMetrics gymMetrics;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        gymMetrics = new GymMetrics(registry);
    }

    @Test
    void recordTraineeRegistration_incrementsTaggedCounter() {
        gymMetrics.recordTraineeRegistration();

        assertEquals(1.0, registry.get("gym.registrations")
                .tag("role", "trainee")
                .counter()
                .count());
        assertEquals(0.0, registry.get("gym.registrations")
                .tag("role", "trainer")
                .counter()
                .count());
    }

    @Test
    void recordTrainerRegistration_incrementsTaggedCounter() {
        gymMetrics.recordTrainerRegistration();

        assertEquals(1.0, registry.get("gym.registrations")
                .tag("role", "trainer")
                .counter()
                .count());
    }

    @Test
    void recordAuthResult_incrementsSuccessOrFailureCounter() {
        gymMetrics.recordAuthResult(true);
        gymMetrics.recordAuthResult(false);
        gymMetrics.recordAuthResult(false);

        assertEquals(1.0, registry.get("gym.auth.attempts")
                .tag("result", "success")
                .counter()
                .count());
        assertEquals(2.0, registry.get("gym.auth.attempts")
                .tag("result", "failure")
                .counter()
                .count());
    }

    @Test
    void recordTrainingCreation_recordsTimerAndReturnsValue() {
        Training training = new Training();

        Training result = gymMetrics.recordTrainingCreation(() -> training);

        assertSame(training, result);
        assertEquals(1L, registry.get("gym.training.creation").timer().count());
    }

    @Test
    void updateMetrics_setsGaugeValues() {
        gymMetrics.updateMetrics(10, 4, 7, 20);

        assertEquals(10.0, registry.get("gym.trainees.total").gauge().value());
        assertEquals(4.0, registry.get("gym.trainers.total").gauge().value());
        assertEquals(7.0, registry.get("gym.active.users.total").gauge().value());
        assertEquals(20.0, registry.get("gym.trainings.total").gauge().value());
    }
}
