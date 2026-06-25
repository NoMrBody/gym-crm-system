package service;

import dao.TrainingDAO;
import model.Training;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TrainingServiceTest {

    @Mock
    private TrainingDAO trainingDAO;

    @InjectMocks
    private TrainingService trainingService;

    @Test
    void testCreateTraining() {
        Training training = new Training();
        training.setTrainingName("Morning Yoga");

        when(trainingDAO.create(any(Training.class))).thenReturn(training);

        Training created = trainingService.create(training);

        assertNotNull(created);
        assertEquals("Morning Yoga", created.getTrainingName());
        verify(trainingDAO, times(1)).create(training);
    }

    @Test
    void testGetById() {
        Training training = new Training();
        training.setId(1L);

        when(trainingDAO.getById(1L)).thenReturn(training);

        Training result = trainingService.getById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(trainingDAO, times(1)).getById(1L);
    }
}