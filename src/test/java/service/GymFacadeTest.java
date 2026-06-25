package service;

import facade.GymFacade;
import model.Trainee;
import model.Trainer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GymFacadeTest {

    @Mock
    private TraineeService traineeService;
    @Mock
    private TrainerService trainerService;
    @Mock
    private TrainingService trainingService;

    @InjectMocks
    private GymFacade gymFacade;

    @Test
    void testCreateTrainee() {
        Trainee trainee = new Trainee();
        gymFacade.createTrainee(trainee);
        verify(traineeService, times(1)).create(trainee);
    }

    @Test
    void testDeleteTrainee() {
        gymFacade.deleteTrainee(1L);
        verify(traineeService, times(1)).delete(1L);
    }

    @Test
    void testGetTrainer() {
        gymFacade.getTrainer(2L);
        verify(trainerService, times(1)).getById(2L);
    }

    @Test
    void testUpdateTrainer() {
        Trainer trainer = new Trainer();
        gymFacade.updateTrainer(trainer);
        verify(trainerService, times(1)).update(trainer);
    }

    @Test
    void testCreateTraining() {
        model.Training training = new model.Training();
        gymFacade.createTraining(training);
        verify(trainingService, times(1)).create(training);
    }

    @Test
    void testGetTraining() {
        gymFacade.getTraining(3L);
        verify(trainingService, times(1)).getById(3L);
    }
}
