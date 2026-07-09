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
    void loginTrainee_delegatesToAuthentication() {
        gymFacade.loginTrainee("Jane.Smith", "pw");
        verify(authenticationService).authenticate("Jane.Smith", "pw");
    }

    @Test
    void loginTrainer_delegatesToAuthentication() {
        gymFacade.loginTrainer("Alice.Cooper", "pw");
        verify(authenticationService).authenticate("Alice.Cooper", "pw");
    }

    @Test
    void getTrainee_delegates() {
        gymFacade.getTrainee("Jane.Smith", "pw");
        verify(traineeService).getByUsername("Jane.Smith", "pw");
    }

    @Test
    void getTrainer_delegates() {
        gymFacade.getTrainer("Alice.Cooper", "pw");
        verify(trainerService).getByUsername("Alice.Cooper", "pw");
    }

    @Test
    void changeTraineePassword_delegates() {
        gymFacade.changeTraineePassword("Jane.Smith", "old", "new");
        verify(traineeService).changePassword("Jane.Smith", "old", "new");
    }

    @Test
    void changeTrainerPassword_delegates() {
        gymFacade.changeTrainerPassword("Alice.Cooper", "old", "new");
        verify(trainerService).changePassword("Alice.Cooper", "old", "new");
    }

    @Test
    void updateTrainee_delegates() {
        Trainee trainee = new Trainee();
        gymFacade.updateTrainee("Jane.Smith", "pw", trainee);
        verify(traineeService).update("Jane.Smith", "pw", trainee);
    }

    @Test
    void updateTrainer_delegates() {
        Trainer trainer = new Trainer();
        gymFacade.updateTrainer("Alice.Cooper", "pw", trainer);
        verify(trainerService).update("Alice.Cooper", "pw", trainer);
    }

    @Test
    void activateTrainee_delegates() {
        gymFacade.activateTrainee("Jane.Smith", "pw");
        verify(traineeService).activate("Jane.Smith", "pw");
    }

    @Test
    void deactivateTrainee_delegates() {
        gymFacade.deactivateTrainee("Jane.Smith", "pw");
        verify(traineeService).deactivate("Jane.Smith", "pw");
    }

    @Test
    void activateTrainer_delegates() {
        gymFacade.activateTrainer("Alice.Cooper", "pw");
        verify(trainerService).activate("Alice.Cooper", "pw");
    }

    @Test
    void deactivateTrainer_delegates() {
        gymFacade.deactivateTrainer("Alice.Cooper", "pw");
        verify(trainerService).deactivate("Alice.Cooper", "pw");
    }

    @Test
    void deleteTrainee_delegates() {
        gymFacade.deleteTrainee("Jane.Smith", "pw");
        verify(traineeService).deleteByUsername("Jane.Smith", "pw");
    }

    @Test
    void getTraineeTrainings_delegates() {
        LocalDate from = LocalDate.of(2024, 1, 1);
        LocalDate to = LocalDate.of(2024, 12, 31);
        gymFacade.getTraineeTrainings("Jane.Smith", "pw", from, to, "Alice.Cooper", "Yoga");
        verify(traineeService).getTrainings("Jane.Smith", "pw", from, to, "Alice.Cooper", "Yoga");
    }

    @Test
    void getTrainerTrainings_delegates() {
        LocalDate from = LocalDate.of(2024, 1, 1);
        LocalDate to = LocalDate.of(2024, 12, 31);
        gymFacade.getTrainerTrainings("Alice.Cooper", "pw", from, to, "Jane.Smith");
        verify(trainerService).getTrainings("Alice.Cooper", "pw", from, to, "Jane.Smith");
    }

    @Test
    void addTraining_delegates() {
        LocalDateTime when = LocalDateTime.of(2024, 6, 1, 10, 0);
        gymFacade.addTraining("Alice.Cooper", "pw", "Jane.Smith", "Alice.Cooper", "Morning Yoga", when, 60);
        verify(trainingService).addTraining("Alice.Cooper", "pw", "Jane.Smith", "Alice.Cooper",
                "Morning Yoga", when, 60);
    }

    @Test
    void getUnassignedTrainers_delegates() {
        gymFacade.getUnassignedTrainers("Jane.Smith", "pw");
        verify(traineeService).getUnassignedTrainers("Jane.Smith", "pw");
    }

    @Test
    void updateTraineeTrainers_delegates() {
        List<String> trainers = List.of("Alice.Cooper");
        gymFacade.updateTraineeTrainers("Jane.Smith", "pw", trainers);
        verify(traineeService).updateTrainers("Jane.Smith", "pw", trainers);
    }
}
