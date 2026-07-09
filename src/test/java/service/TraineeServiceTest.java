package service;

import dao.TraineeDAO;
import dao.TrainerDAO;
import dao.TrainingDAO;
import exception.AuthenticationException;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TraineeServiceTest {

    @Mock
    private TraineeDAO traineeDAO;
    @Mock
    private TrainerDAO trainerDAO;
    @Mock
    private TrainingDAO trainingDAO;
    @Mock
    private ProfileService profileService;
    @Mock
    private AuthenticationService authenticationService;

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
        when(profileService.generateUsername("Jane", "Smith")).thenReturn("Jane.Smith");
        when(profileService.generatePassword()).thenReturn("ABCDE12345");
        when(traineeDAO.save(trainee)).thenReturn(trainee);

        Trainee created = traineeService.create(trainee);

        assertEquals("Jane.Smith", created.getUser().getUsername());
        assertEquals(10, created.getUser().getPassword().length());
        assertTrue(created.getUser().isActive());
        verify(traineeDAO).save(trainee);
    }

    @Test
    void create_invalidUser_throwsValidationAndDoesNotSave() {
        Trainee trainee = new Trainee();
        trainee.setUser(new User()); // missing firstName/lastName

        assertThrows(ValidationException.class, () -> traineeService.create(trainee));
        verify(traineeDAO, never()).save(any());
    }

    @Test
    void getByUsername_authenticatedReturnsTrainee() {
        Trainee trainee = traineeWith("Jane", "Smith", true);
        when(traineeDAO.findByUsername("Jane.Smith")).thenReturn(Optional.of(trainee));

        Trainee result = traineeService.getByUsername("Jane.Smith", "pw");

        assertSame(trainee, result);
        verify(authenticationService).authenticate("Jane.Smith", "pw");
    }

    @Test
    void getByUsername_authFailure_doesNotQueryDao() {
        doThrow(new AuthenticationException("bad")).when(authenticationService).authenticate("Jane.Smith", "wrong");

        assertThrows(AuthenticationException.class, () -> traineeService.getByUsername("Jane.Smith", "wrong"));
        verify(traineeDAO, never()).findByUsername(anyString());
    }

    @Test
    void update_updatesMutableFields() {
        Trainee existing = traineeWith("Old", "Name", true);
        Trainee updatedData = traineeWith("New", "Name", true);
        updatedData.setDateOfBirth(LocalDate.of(1990, 5, 5));
        updatedData.setAddress("42 New St");
        when(traineeDAO.findByUsername("Old.Name")).thenReturn(Optional.of(existing));
        when(traineeDAO.save(existing)).thenReturn(existing);

        Trainee result = traineeService.update("Old.Name", "pw", updatedData);

        assertEquals("New", result.getUser().getFirstName());
        assertEquals(LocalDate.of(1990, 5, 5), result.getDateOfBirth());
        assertEquals("42 New St", result.getAddress());
        verify(traineeDAO).save(existing);
    }

    @Test
    void changePassword_setsNewPassword() {
        Trainee trainee = traineeWith("Jane", "Smith", true);
        when(traineeDAO.findByUsername("Jane.Smith")).thenReturn(Optional.of(trainee));

        traineeService.changePassword("Jane.Smith", "old", "newSecret");

        assertEquals("newSecret", trainee.getUser().getPassword());
        verify(traineeDAO).save(trainee);
    }

    @Test
    void changePassword_blankNewPassword_throwsValidation() {
        assertThrows(ValidationException.class,
                () -> traineeService.changePassword("Jane.Smith", "old", "  "));
        verify(traineeDAO, never()).save(any());
    }

    @Test
    void activate_inactiveTrainee_activates() {
        Trainee trainee = traineeWith("Jane", "Smith", false);
        when(traineeDAO.findByUsername("Jane.Smith")).thenReturn(Optional.of(trainee));

        traineeService.activate("Jane.Smith", "pw");

        assertTrue(trainee.getUser().isActive());
        verify(traineeDAO).save(trainee);
    }

    @Test
    void activate_alreadyActive_throwsValidationAndDoesNotSave() {
        Trainee trainee = traineeWith("Jane", "Smith", true);
        when(traineeDAO.findByUsername("Jane.Smith")).thenReturn(Optional.of(trainee));

        assertThrows(ValidationException.class, () -> traineeService.activate("Jane.Smith", "pw"));
        verify(traineeDAO, never()).save(any());
    }

    @Test
    void deactivate_activeTrainee_deactivates() {
        Trainee trainee = traineeWith("Jane", "Smith", true);
        when(traineeDAO.findByUsername("Jane.Smith")).thenReturn(Optional.of(trainee));

        traineeService.deactivate("Jane.Smith", "pw");

        assertFalse(trainee.getUser().isActive());
        verify(traineeDAO).save(trainee);
    }

    @Test
    void deactivate_alreadyInactive_throwsValidationAndDoesNotSave() {
        Trainee trainee = traineeWith("Jane", "Smith", false);
        when(traineeDAO.findByUsername("Jane.Smith")).thenReturn(Optional.of(trainee));

        assertThrows(ValidationException.class, () -> traineeService.deactivate("Jane.Smith", "pw"));
        verify(traineeDAO, never()).save(any());
    }

    @Test
    void deleteByUsername_authenticatesThenDeletes() {
        traineeService.deleteByUsername("Jane.Smith", "pw");

        verify(authenticationService).authenticate("Jane.Smith", "pw");
        verify(traineeDAO).deleteByUsername("Jane.Smith");
    }

    @Test
    void getTrainings_delegatesToTrainingDao() {
        LocalDate from = LocalDate.of(2024, 1, 1);
        LocalDate to = LocalDate.of(2024, 12, 31);
        when(trainingDAO.findTraineeTrainings("Jane.Smith", from, to, "Alice.Cooper", "Yoga"))
                .thenReturn(List.of(new Training()));

        List<Training> result =
                traineeService.getTrainings("Jane.Smith", "pw", from, to, "Alice.Cooper", "Yoga");

        assertEquals(1, result.size());
        verify(authenticationService).authenticate("Jane.Smith", "pw");
    }

    @Test
    void getUnassignedTrainers_delegatesToTraineeDao() {
        when(traineeDAO.findUnassignedTrainers("Jane.Smith")).thenReturn(List.of(new Trainer()));

        List<Trainer> result = traineeService.getUnassignedTrainers("Jane.Smith", "pw");

        assertEquals(1, result.size());
    }

    @Test
    void updateTrainers_replacesTrainerSet() {
        Trainee trainee = traineeWith("Jane", "Smith", true);
        Trainer trainer = new Trainer();
        User trainerUser = new User();
        trainerUser.setUsername("Alice.Cooper");
        trainer.setUser(trainerUser);
        when(traineeDAO.findByUsername("Jane.Smith")).thenReturn(Optional.of(trainee));
        when(trainerDAO.findByUsername("Alice.Cooper")).thenReturn(Optional.of(trainer));
        when(traineeDAO.save(trainee)).thenReturn(trainee);

        Trainee result = traineeService.updateTrainers("Jane.Smith", "pw", List.of("Alice.Cooper"));

        assertEquals(1, result.getTrainers().size());
        assertTrue(result.getTrainers().contains(trainer));
    }

    @Test
    void updateTrainers_unknownTrainer_throwsEntityNotFound() {
        Trainee trainee = traineeWith("Jane", "Smith", true);
        when(traineeDAO.findByUsername("Jane.Smith")).thenReturn(Optional.of(trainee));
        when(trainerDAO.findByUsername("Ghost.Trainer")).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> traineeService.updateTrainers("Jane.Smith", "pw", List.of("Ghost.Trainer")));
        verify(traineeDAO, never()).save(any());
    }
}
