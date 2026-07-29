package health;

import model.TrainingType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.health.contributor.Status;
import repository.TrainingTypeRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrainingTypesHealthIndicatorTest {

    @Mock
    private TrainingTypeRepository trainingTypeRepository;

    @InjectMocks
    private TrainingTypesHealthIndicator indicator;

    @Test
    void health_whenTypesExist_returnsUp() {
        TrainingType yoga = new TrainingType();
        yoga.setTrainingTypeName("Yoga");
        when(trainingTypeRepository.findAll()).thenReturn(List.of(yoga));

        var health = indicator.health();

        assertEquals(Status.UP, health.getStatus());
        assertEquals(1, health.getDetails().get("count"));
    }

    @Test
    void health_whenTypesEmpty_returnsDown() {
        when(trainingTypeRepository.findAll()).thenReturn(List.of());

        var health = indicator.health();

        assertEquals(Status.DOWN, health.getStatus());
        assertEquals("no training types seeded", health.getDetails().get("message"));
    }
}
