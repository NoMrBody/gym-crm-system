package service;

import dao.TraineeDAO;
import dao.TrainerDAO;
import dao.TrainingDAO;
import exception.ValidationException;
import jakarta.persistence.EntityNotFoundException;
import model.Trainee;
import model.Trainer;
import model.Training;
import model.TrainingType;
import model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TrainingServiceTest {

    @Mock
    private TrainingDAO trainingDAO;
    @Mock
    private TraineeDAO traineeDAO;
    @Mock
    private TrainerDAO trainerDAO;

    @InjectMocks
    private TrainingService trainingService;

    private Trainee trainee;
    private Trainer trainer;
    private TrainingType specialization;

    @BeforeEach
    void setUp() {
        User traineeUser = new User();
        traineeUser.setUsername("Jane.Smith");
        trainee = new Trainee();
        trainee.setUser(traineeUser);

        specialization = new TrainingType();
        specialization.setTrainingTypeName("Yoga");
        User trainerUser = new User();
        trainerUser.setUsername("Alice.Cooper");
        trainer = new Trainer();
        trainer.setUser(trainerUser);
        trainer.setSpecialization(specialization);
    }

    @Test
    void addTraining_persistsAndLinksBothSides() {
        LocalDateTime when = LocalDateTime.of(2024, 6, 1, 10, 0);
        when(traineeDAO.findByUsername("Jane.Smith")).thenReturn(Optional.of(trainee));
        when(trainerDAO.findByUsername("Alice.Cooper")).thenReturn(Optional.of(trainer));
        when(trainingDAO.save(any(Training.class))).thenAnswer(inv -> inv.getArgument(0));

        Training result = trainingService.addTraining(
                "Jane.Smith", "Alice.Cooper", "Morning Yoga", when, 60);

        assertEquals("Morning Yoga", result.getTrainingName());
        assertSame(trainee, result.getTrainee());
        assertSame(trainer, result.getTrainer());
        assertSame(specialization, result.getTrainingType());
        assertEquals(60, result.getTrainingDuration());
        assertTrue(trainee.getTrainers().contains(trainer));
        assertTrue(trainer.getTrainees().contains(trainee));
    }

    @Test
    void addTraining_traineeNotFound_throwsEntityNotFound() {
        when(traineeDAO.findByUsername("Ghost.User")).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> trainingService.addTraining(
                "Ghost.User", "Alice.Cooper", "Morning Yoga", LocalDateTime.now(), 60));
        verify(trainingDAO, never()).save(any());
    }

    @Test
    void addTraining_trainerNotFound_throwsEntityNotFound() {
        when(traineeDAO.findByUsername("Jane.Smith")).thenReturn(Optional.of(trainee));
        when(trainerDAO.findByUsername("Ghost.Trainer")).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> trainingService.addTraining(
                "Jane.Smith", "Ghost.Trainer", "Morning Yoga", LocalDateTime.now(), 60));
        verify(trainingDAO, never()).save(any());
    }

    @Test
    void addTraining_blankTrainingName_throwsValidation() {
        when(traineeDAO.findByUsername("Jane.Smith")).thenReturn(Optional.of(trainee));
        when(trainerDAO.findByUsername("Alice.Cooper")).thenReturn(Optional.of(trainer));

        assertThrows(ValidationException.class, () -> trainingService.addTraining(
                "Jane.Smith", "Alice.Cooper", "  ", LocalDateTime.now(), 60));
        verify(trainingDAO, never()).save(any());
    }
}
