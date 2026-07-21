package service;

import facade.GymFacade;
import model.Trainee;
import model.Trainer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GymFacadeTest {

    @Mock
    private TraineeService traineeService;
    @Mock
    private TrainerService trainerService;
    @Mock
    private TrainingService trainingService;
    @Mock
    private TrainingTypeService trainingTypeService;
    @Mock
    private AuthenticationService authenticationService;

    @InjectMocks
    private GymFacade gymFacade;

    @Test
    void createTrainee_delegates() {
        Trainee trainee = new Trainee();
        gymFacade.createTrainee(trainee);
        verify(traineeService).create(trainee);
    }

    @Test
    void createTrainer_delegates() {
        Trainer trainer = new Trainer();
        gymFacade.createTrainer(trainer);
        verify(trainerService).create(trainer);
    }

    @Test
    void login_delegatesToAuthentication() {
        gymFacade.login("Jane.Smith", "pw");
        verify(authenticationService).authenticate("Jane.Smith", "pw");
    }

    @Test
    void changeLogin_delegatesToAuthentication() {
        gymFacade.changeLogin("Jane.Smith", "old", "new");
        verify(authenticationService).changeLogin("Jane.Smith", "old", "new");
    }

    @Test
    void getTrainee_delegates() {
        gymFacade.getTrainee("Jane.Smith");
        verify(traineeService).getByUsername("Jane.Smith");
    }

    @Test
    void getTrainer_delegates() {
        gymFacade.getTrainer("Alice.Cooper");
        verify(trainerService).getByUsername("Alice.Cooper");
    }

    @Test
    void updateTrainee_delegates() {
        Trainee trainee = new Trainee();
        gymFacade.updateTrainee("Jane.Smith", trainee);
        verify(traineeService).update("Jane.Smith", trainee);
    }

    @Test
    void updateTrainer_delegates() {
        Trainer trainer = new Trainer();
        gymFacade.updateTrainer("Alice.Cooper", trainer);
        verify(trainerService).update("Alice.Cooper", trainer);
    }

    @Test
    void setTraineeActive_true_activates() {
        gymFacade.setTraineeActive("Jane.Smith", true);
        verify(traineeService).activate("Jane.Smith");
    }

    @Test
    void setTraineeActive_false_deactivates() {
        gymFacade.setTraineeActive("Jane.Smith", false);
        verify(traineeService).deactivate("Jane.Smith");
    }

    @Test
    void setTrainerActive_true_activates() {
        gymFacade.setTrainerActive("Alice.Cooper", true);
        verify(trainerService).activate("Alice.Cooper");
    }

    @Test
    void setTrainerActive_false_deactivates() {
        gymFacade.setTrainerActive("Alice.Cooper", false);
        verify(trainerService).deactivate("Alice.Cooper");
    }

    @Test
    void deleteTrainee_delegates() {
        gymFacade.deleteTrainee("Jane.Smith");
        verify(traineeService).deleteByUsername("Jane.Smith");
    }

    @Test
    void getTraineeTrainings_delegates() {
        LocalDate from = LocalDate.of(2024, 1, 1);
        LocalDate to = LocalDate.of(2024, 12, 31);
        gymFacade.getTraineeTrainings("Jane.Smith", from, to, "Alice.Cooper", "Yoga");
        verify(traineeService).getTrainings("Jane.Smith", from, to, "Alice.Cooper", "Yoga");
    }

    @Test
    void getTrainerTrainings_delegates() {
        LocalDate from = LocalDate.of(2024, 1, 1);
        LocalDate to = LocalDate.of(2024, 12, 31);
        gymFacade.getTrainerTrainings("Alice.Cooper", from, to, "Jane.Smith");
        verify(trainerService).getTrainings("Alice.Cooper", from, to, "Jane.Smith");
    }

    @Test
    void addTraining_delegates() {
        LocalDateTime when = LocalDateTime.of(2024, 6, 1, 10, 0);
        gymFacade.addTraining("Jane.Smith", "Alice.Cooper", "Morning Yoga", when, 60);
        verify(trainingService).addTraining("Jane.Smith", "Alice.Cooper", "Morning Yoga", when, 60);
    }

    @Test
    void getUnassignedTrainers_delegates() {
        gymFacade.getUnassignedTrainers("Jane.Smith");
        verify(traineeService).getUnassignedTrainers("Jane.Smith");
    }

    @Test
    void updateTraineeTrainers_delegates() {
        List<String> trainers = List.of("Alice.Cooper");
        gymFacade.updateTraineeTrainers("Jane.Smith", trainers);
        verify(traineeService).updateTrainers("Jane.Smith", trainers);
    }

    @Test
    void getTrainingTypes_delegates() {
        gymFacade.getTrainingTypes();
        verify(trainingTypeService).getAll();
    }
}
