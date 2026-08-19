package service;

import exception.ValidationException;
import jakarta.persistence.EntityNotFoundException;
import model.Trainee;
import model.Trainer;
import model.Training;
import model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import repository.TraineeRepository;
import repository.TrainerRepository;
import repository.TrainingRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TraineeServiceTest {

    @Mock
    private TraineeRepository traineeRepository;
    @Mock
    private TrainerRepository trainerRepository;
    @Mock
    private TrainingRepository trainingRepository;
    @Mock
    private ProfileService profileService;

    @InjectMocks
    private TraineeService traineeService;

    private static Trainee traineeWith(String firstName, String lastName, boolean active) {
        User user = new User();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setUsername(firstName + "." + lastName);
        user.setActive(active);
        Trainee trainee = new Trainee();
        trainee.setUser(user);
        return trainee;
    }

    @Test
    void create_generatesCredentialsAndActivates() {
        Trainee trainee = traineeWith("Jane", "Smith", false);
        when(profileService.assignCredentials(trainee.getUser())).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setUsername("Jane.Smith");
            user.setPassword("{bcrypt}$2a$10$hash");
            return "ABCDE12345";
        });
        when(traineeRepository.save(trainee)).thenReturn(trainee);

        RegistrationResult<Trainee> created = traineeService.create(trainee);

        assertEquals("Jane.Smith", created.profile().getUser().getUsername());
        assertEquals("ABCDE12345", created.rawPassword());
        assertEquals("{bcrypt}$2a$10$hash", created.profile().getUser().getPassword());
        assertTrue(created.profile().getUser().isActive());
        verify(traineeRepository).save(trainee);
    }

    @Test
    void create_invalidUser_throwsValidationAndDoesNotSave() {
        Trainee trainee = new Trainee();
        trainee.setUser(new User()); // missing firstName/lastName

        assertThrows(ValidationException.class, () -> traineeService.create(trainee));
        verify(traineeRepository, never()).save(any());
    }

    @Test
    void getByUsername_returnsTrainee() {
        Trainee trainee = traineeWith("Jane", "Smith", true);
        when(traineeRepository.findByUser_Username("Jane.Smith")).thenReturn(Optional.of(trainee));

        Trainee result = traineeService.getByUsername("Jane.Smith");

        assertSame(trainee, result);
    }

    @Test
    void getByUsername_notFound_throwsEntityNotFound() {
        when(traineeRepository.findByUser_Username("Ghost.User")).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> traineeService.getByUsername("Ghost.User"));
    }

    @Test
    void update_updatesMutableFieldsIncludingActive() {
        Trainee existing = traineeWith("Old", "Name", true);
        Trainee updatedData = traineeWith("New", "Name", false);
        updatedData.setDateOfBirth(LocalDate.of(1990, 5, 5));
        updatedData.setAddress("42 New St");
        when(traineeRepository.findByUser_Username("Old.Name")).thenReturn(Optional.of(existing));
        when(traineeRepository.save(existing)).thenReturn(existing);

        Trainee result = traineeService.update("Old.Name", updatedData);

        assertEquals("New", result.getUser().getFirstName());
        assertFalse(result.getUser().isActive());
        assertEquals(LocalDate.of(1990, 5, 5), result.getDateOfBirth());
        assertEquals("42 New St", result.getAddress());
        verify(traineeRepository).save(existing);
    }

    @Test
    void activate_inactiveTrainee_activates() {
        Trainee trainee = traineeWith("Jane", "Smith", false);
        when(traineeRepository.findByUser_Username("Jane.Smith")).thenReturn(Optional.of(trainee));

        traineeService.activate("Jane.Smith");

        assertTrue(trainee.getUser().isActive());
        verify(traineeRepository).save(trainee);
    }

    @Test
    void activate_alreadyActive_throwsValidationAndDoesNotSave() {
        Trainee trainee = traineeWith("Jane", "Smith", true);
        when(traineeRepository.findByUser_Username("Jane.Smith")).thenReturn(Optional.of(trainee));

        assertThrows(ValidationException.class, () -> traineeService.activate("Jane.Smith"));
        verify(traineeRepository, never()).save(any());
    }

    @Test
    void deactivate_activeTrainee_deactivates() {
        Trainee trainee = traineeWith("Jane", "Smith", true);
        when(traineeRepository.findByUser_Username("Jane.Smith")).thenReturn(Optional.of(trainee));

        traineeService.deactivate("Jane.Smith");

        assertFalse(trainee.getUser().isActive());
        verify(traineeRepository).save(trainee);
    }

    @Test
    void deactivate_alreadyInactive_throwsValidationAndDoesNotSave() {
        Trainee trainee = traineeWith("Jane", "Smith", false);
        when(traineeRepository.findByUser_Username("Jane.Smith")).thenReturn(Optional.of(trainee));

        assertThrows(ValidationException.class, () -> traineeService.deactivate("Jane.Smith"));
        verify(traineeRepository, never()).save(any());
    }

    @Test
    void deleteByUsername_deletesWhenPresent() {
        Trainee trainee = traineeWith("Jane", "Smith", true);
        when(traineeRepository.findByUser_Username("Jane.Smith")).thenReturn(Optional.of(trainee));

        traineeService.deleteByUsername("Jane.Smith");

        verify(traineeRepository).delete(trainee);
    }

    @Test
    void deleteByUsername_notFound_throwsEntityNotFound() {
        when(traineeRepository.findByUser_Username("Ghost.User")).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> traineeService.deleteByUsername("Ghost.User"));
        verify(traineeRepository, never()).delete(any(Trainee.class));
    }

    @Test
    void getTrainings_delegatesToTrainingRepository() {
        Trainee trainee = traineeWith("Jane", "Smith", true);
        LocalDate from = LocalDate.of(2024, 1, 1);
        LocalDate to = LocalDate.of(2024, 12, 31);
        when(traineeRepository.findByUser_Username("Jane.Smith")).thenReturn(Optional.of(trainee));
        when(trainingRepository.findTraineeTrainings("Jane.Smith", from, to, "Alice.Cooper", "Yoga"))
                .thenReturn(List.of(new Training()));

        List<Training> result =
                traineeService.getTrainings("Jane.Smith", from, to, "Alice.Cooper", "Yoga");

        assertEquals(1, result.size());
    }

    @Test
    void getUnassignedTrainers_delegatesToTrainerRepository() {
        Trainee trainee = traineeWith("Jane", "Smith", true);
        when(traineeRepository.findByUser_Username("Jane.Smith")).thenReturn(Optional.of(trainee));
        when(trainerRepository.findUnassignedFor("Jane.Smith")).thenReturn(List.of(new Trainer()));

        List<Trainer> result = traineeService.getUnassignedTrainers("Jane.Smith");

        assertEquals(1, result.size());
    }

    @Test
    void updateTrainers_replacesTrainerSet() {
        Trainee trainee = traineeWith("Jane", "Smith", true);
        Trainer trainer = new Trainer();
        User trainerUser = new User();
        trainerUser.setUsername("Alice.Cooper");
        trainer.setUser(trainerUser);
        when(traineeRepository.findByUser_Username("Jane.Smith")).thenReturn(Optional.of(trainee));
        when(trainerRepository.findByUser_Username("Alice.Cooper")).thenReturn(Optional.of(trainer));
        when(traineeRepository.save(trainee)).thenReturn(trainee);

        Trainee result = traineeService.updateTrainers("Jane.Smith", List.of("Alice.Cooper"));

        assertEquals(1, result.getTrainers().size());
        assertTrue(result.getTrainers().contains(trainer));
    }

    @Test
    void updateTrainers_unknownTrainer_throwsEntityNotFound() {
        Trainee trainee = traineeWith("Jane", "Smith", true);
        when(traineeRepository.findByUser_Username("Jane.Smith")).thenReturn(Optional.of(trainee));
        when(trainerRepository.findByUser_Username("Ghost.Trainer")).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> traineeService.updateTrainers("Jane.Smith", List.of("Ghost.Trainer")));
        verify(traineeRepository, never()).save(any());
    }
}
