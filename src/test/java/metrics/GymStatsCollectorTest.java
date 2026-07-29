package metrics;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import repository.TraineeRepository;
import repository.TrainerRepository;
import repository.TrainingRepository;
import repository.UserRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GymStatsCollectorTest {

    @Mock
    private TraineeRepository traineeRepository;
    @Mock
    private TrainerRepository trainerRepository;
    @Mock
    private TrainingRepository trainingRepository;
    @Mock
    private UserRepository userRepository;

    private SimpleMeterRegistry registry;
    private GymMetrics gymMetrics;
    private GymStatsCollector collector;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        gymMetrics = new GymMetrics(registry);
        collector = new GymStatsCollector(
                traineeRepository,
                trainerRepository,
                trainingRepository,
                userRepository,
                gymMetrics);
    }

    @Test
    void collectStats_refreshesGaugesFromRepositories() {
        when(traineeRepository.count()).thenReturn(12L);
        when(trainerRepository.count()).thenReturn(5L);
        when(trainingRepository.count()).thenReturn(30L);
        when(userRepository.countByIsActiveTrue()).thenReturn(9L);

        collector.collectStats();

        assertEquals(12.0, registry.get("gym.trainees.total").gauge().value());
        assertEquals(5.0, registry.get("gym.trainers.total").gauge().value());
        assertEquals(30.0, registry.get("gym.trainings.total").gauge().value());
        assertEquals(9.0, registry.get("gym.active.users.total").gauge().value());
    }
}
