package health;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.health.contributor.Status;
import repository.TraineeRepository;
import repository.TrainerRepository;
import repository.TrainingRepository;
import repository.UserRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DatabaseHealthIndicatorTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private TraineeRepository traineeRepository;
    @Mock
    private TrainerRepository trainerRepository;
    @Mock
    private TrainingRepository trainingRepository;

    @InjectMocks
    private DatabaseHealthIndicator indicator;

    @Test
    void health_whenCountsSucceed_returnsUpWithDetails() {
        when(userRepository.count()).thenReturn(10L);
        when(traineeRepository.count()).thenReturn(4L);
        when(trainerRepository.count()).thenReturn(3L);
        when(trainingRepository.count()).thenReturn(7L);

        var health = indicator.health();

        assertEquals(Status.UP, health.getStatus());
        assertEquals(10L, health.getDetails().get("users"));
        assertEquals(4L, health.getDetails().get("trainees"));
        assertEquals(3L, health.getDetails().get("trainers"));
        assertEquals(7L, health.getDetails().get("trainings"));
    }

    @Test
    void health_whenRepositoryThrows_returnsDown() {
        when(userRepository.count()).thenThrow(new RuntimeException("connection refused"));

        var health = indicator.health();

        assertEquals(Status.DOWN, health.getStatus());
    }
}
