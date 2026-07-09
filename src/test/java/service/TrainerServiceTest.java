package service;

import dao.TrainerDAO;
import dao.TrainingDAO;
import dao.TrainingTypeDAO;
import exception.AuthenticationException;
import exception.ValidationException;
import jakarta.persistence.EntityNotFoundException;
import model.Trainer;
import model.Training;
import model.TrainingType;
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
class TrainerServiceTest {

    @Mock
    private TrainerDAO trainerDAO;
    @Mock
    private TrainingDAO trainingDAO;
    @Mock
    private TrainingTypeDAO trainingTypeDAO;
    @Mock
    private ProfileService profileService;
    @Mock
    private AuthenticationService authenticationService;

    @InjectMocks
    private TrainerService trainerService;

    private static TrainingType type(String name) {
        TrainingType t = new TrainingType();
        t.setTrainingTypeName(name);
        return t;
    }

    private static Trainer trainerWith(String firstName, String lastName, boolean active, TrainingType spec) {
        User user = new User();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setUsername(firstName + "." + lastName);
        user.setActive(active);
        Trainer trainer = new Trainer();
        trainer.setUser(user);
        trainer.setSpecialization(spec);
        return trainer;
    }

    @Test
    void create_resolvesSpecializationGeneratesCredentialsAndActivates() {
        TrainingType resolved = type("Yoga");
        Trainer trainer = trainerWith("Alice", "Cooper", false, type("Yoga"));
        when(trainingTypeDAO.findByName("Yoga")).thenReturn(Optional.of(resolved));
        when(profileService.generateUsername("Alice", "Cooper")).thenReturn("Alice.Cooper");
        when(profileService.generatePassword()).thenReturn("ABCDE12345");
        when(trainerDAO.save(trainer)).thenReturn(trainer);

        Trainer created = trainerService.create(trainer);

        assertEquals("Alice.Cooper", created.getUser().getUsername());
        assertEquals(10, created.getUser().getPassword().length());
        assertTrue(created.getUser().isActive());
        assertSame(resolved, created.getSpecialization());
        verify(trainerDAO).save(trainer);
    }

    @Test
    void create_unknownSpecialization_throwsEntityNotFound() {
        Trainer trainer = trainerWith("Alice", "Cooper", false, type("Unknown"));
        when(trainingTypeDAO.findByName("Unknown")).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> trainerService.create(trainer));
        verify(trainerDAO, never()).save(any());
    }

    @Test
    void create_missingUser_throwsValidation() {
        Trainer trainer = new Trainer();
        trainer.setUser(new User());
        trainer.setSpecialization(type("Yoga"));

        assertThrows(ValidationException.class, () -> trainerService.create(trainer));
        verify(trainerDAO, never()).save(any());
    }

    @Test
    void getByUsername_authenticatedReturnsTrainer() {
        Trainer trainer = trainerWith("Alice", "Cooper", true, type("Yoga"));
        when(trainerDAO.findByUsername("Alice.Cooper")).thenReturn(Optional.of(trainer));

        Trainer result = trainerService.getByUsername("Alice.Cooper", "pw");

        assertSame(trainer, result);
        verify(authenticationService).authenticate("Alice.Cooper", "pw");
    }

    @Test
    void getByUsername_authFailure_doesNotQueryDao() {
        doThrow(new AuthenticationException("bad")).when(authenticationService).authenticate("Alice.Cooper", "wrong");

        assertThrows(AuthenticationException.class, () -> trainerService.getByUsername("Alice.Cooper", "wrong"));
        verify(trainerDAO, never()).findByUsername(anyString());
    }

    @Test
    void update_updatesNameAndSpecialization() {
        Trainer existing = trainerWith("Alice", "Cooper", true, type("Yoga"));
        Trainer updatedData = trainerWith("Alicia", "Cooper", true, type("Pilates"));
        TrainingType resolved = type("Pilates");
        when(trainerDAO.findByUsername("Alice.Cooper")).thenReturn(Optional.of(existing));
        when(trainingTypeDAO.findByName("Pilates")).thenReturn(Optional.of(resolved));
        when(trainerDAO.save(existing)).thenReturn(existing);

        Trainer result = trainerService.update("Alice.Cooper", "pw", updatedData);

        assertEquals("Alicia", result.getUser().getFirstName());
        assertSame(resolved, result.getSpecialization());
        verify(trainerDAO).save(existing);
    }

    @Test
    void changePassword_setsNewPassword() {
        Trainer trainer = trainerWith("Alice", "Cooper", true, type("Yoga"));
        when(trainerDAO.findByUsername("Alice.Cooper")).thenReturn(Optional.of(trainer));

        trainerService.changePassword("Alice.Cooper", "old", "newSecret");

        assertEquals("newSecret", trainer.getUser().getPassword());
        verify(trainerDAO).save(trainer);
    }

    @Test
    void activate_alreadyActive_throwsValidation() {
        Trainer trainer = trainerWith("Alice", "Cooper", true, type("Yoga"));
        when(trainerDAO.findByUsername("Alice.Cooper")).thenReturn(Optional.of(trainer));

        assertThrows(ValidationException.class, () -> trainerService.activate("Alice.Cooper", "pw"));
        verify(trainerDAO, never()).save(any());
    }

    @Test
    void deactivate_activeTrainer_deactivates() {
        Trainer trainer = trainerWith("Alice", "Cooper", true, type("Yoga"));
        when(trainerDAO.findByUsername("Alice.Cooper")).thenReturn(Optional.of(trainer));

        trainerService.deactivate("Alice.Cooper", "pw");

        assertFalse(trainer.getUser().isActive());
        verify(trainerDAO).save(trainer);
    }

    @Test
    void deactivate_alreadyInactive_throwsValidation() {
        Trainer trainer = trainerWith("Alice", "Cooper", false, type("Yoga"));
        when(trainerDAO.findByUsername("Alice.Cooper")).thenReturn(Optional.of(trainer));

        assertThrows(ValidationException.class, () -> trainerService.deactivate("Alice.Cooper", "pw"));
        verify(trainerDAO, never()).save(any());
    }

    @Test
    void getTrainings_delegatesToTrainingDao() {
        LocalDate from = LocalDate.of(2024, 1, 1);
        LocalDate to = LocalDate.of(2024, 12, 31);
        when(trainingDAO.findTrainerTrainings("Alice.Cooper", from, to, "Jane.Smith"))
                .thenReturn(List.of(new Training()));

        List<Training> result =
                trainerService.getTrainings("Alice.Cooper", "pw", from, to, "Jane.Smith");

        assertEquals(1, result.size());
        verify(authenticationService).authenticate("Alice.Cooper", "pw");
    }
}
