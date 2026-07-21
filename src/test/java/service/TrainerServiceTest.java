package service;

import dao.TrainerDAO;
import dao.TrainingDAO;
import dao.TrainingTypeDAO;
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
    void getByUsername_returnsTrainer() {
        Trainer trainer = trainerWith("Alice", "Cooper", true, type("Yoga"));
        when(trainerDAO.findByUsername("Alice.Cooper")).thenReturn(Optional.of(trainer));

        Trainer result = trainerService.getByUsername("Alice.Cooper");

        assertSame(trainer, result);
    }

    @Test
    void getByUsername_notFound_throwsEntityNotFound() {
        when(trainerDAO.findByUsername("Ghost.Trainer")).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> trainerService.getByUsername("Ghost.Trainer"));
    }

    @Test
    void update_updatesNameAndActiveButNotSpecialization() {
        TrainingType original = type("Yoga");
        Trainer existing = trainerWith("Alice", "Cooper", true, original);
        Trainer updatedData = trainerWith("Alicia", "Cooper", false, null);
        when(trainerDAO.findByUsername("Alice.Cooper")).thenReturn(Optional.of(existing));
        when(trainerDAO.save(existing)).thenReturn(existing);

        Trainer result = trainerService.update("Alice.Cooper", updatedData);

        assertEquals("Alicia", result.getUser().getFirstName());
        assertFalse(result.getUser().isActive());
        assertSame(original, result.getSpecialization());
        verify(trainerDAO).save(existing);
    }

    @Test
    void activate_alreadyActive_throwsValidation() {
        Trainer trainer = trainerWith("Alice", "Cooper", true, type("Yoga"));
        when(trainerDAO.findByUsername("Alice.Cooper")).thenReturn(Optional.of(trainer));

        assertThrows(ValidationException.class, () -> trainerService.activate("Alice.Cooper"));
        verify(trainerDAO, never()).save(any());
    }

    @Test
    void deactivate_activeTrainer_deactivates() {
        Trainer trainer = trainerWith("Alice", "Cooper", true, type("Yoga"));
        when(trainerDAO.findByUsername("Alice.Cooper")).thenReturn(Optional.of(trainer));

        trainerService.deactivate("Alice.Cooper");

        assertFalse(trainer.getUser().isActive());
        verify(trainerDAO).save(trainer);
    }

    @Test
    void deactivate_alreadyInactive_throwsValidation() {
        Trainer trainer = trainerWith("Alice", "Cooper", false, type("Yoga"));
        when(trainerDAO.findByUsername("Alice.Cooper")).thenReturn(Optional.of(trainer));

        assertThrows(ValidationException.class, () -> trainerService.deactivate("Alice.Cooper"));
        verify(trainerDAO, never()).save(any());
    }

    @Test
    void getTrainings_delegatesToTrainingDao() {
        Trainer trainer = trainerWith("Alice", "Cooper", true, type("Yoga"));
        LocalDate from = LocalDate.of(2024, 1, 1);
        LocalDate to = LocalDate.of(2024, 12, 31);
        when(trainerDAO.findByUsername("Alice.Cooper")).thenReturn(Optional.of(trainer));
        when(trainingDAO.findTrainerTrainings("Alice.Cooper", from, to, "Jane.Smith"))
                .thenReturn(List.of(new Training()));

        List<Training> result =
                trainerService.getTrainings("Alice.Cooper", from, to, "Jane.Smith");

        assertEquals(1, result.size());
    }
}
