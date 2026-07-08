package service;

import dao.TraineeDAO;
import dao.TrainerDAO;
import model.Trainee;
import model.Trainer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock
    private TraineeDAO traineeDAO;

    @Mock
    private TrainerDAO trainerDAO;

    @InjectMocks
    private ProfileService profileService;

    @Test
    void generatesPlainUsername_whenNoConflict() {
        when(traineeDAO.getAll()).thenReturn(new ArrayList<>());
        when(trainerDAO.getAll()).thenReturn(new ArrayList<>());

        assertEquals("Jane.Smith", profileService.generateUsername("Jane", "Smith"));
    }

    @Test
    void appendsSerial_whenTraineeHasSameName() {
        Trainee existing = new Trainee();
        existing.setUsername("Jane.Smith");

        when(traineeDAO.getAll()).thenReturn(List.of(existing));
        when(trainerDAO.getAll()).thenReturn(new ArrayList<>());

        assertEquals("Jane.Smith1", profileService.generateUsername("Jane", "Smith"));
    }

    @Test
    void appendsSerial_whenTrainerHasSameName() {
        Trainer existing = new Trainer();
        existing.setUsername("John.Smith");

        when(traineeDAO.getAll()).thenReturn(new ArrayList<>());
        when(trainerDAO.getAll()).thenReturn(List.of(existing));

        assertEquals("John.Smith1", profileService.generateUsername("John", "Smith"));
    }

    @Test
    void generatesTenCharPassword() {
        assertEquals(10, profileService.generatePassword().length());
    }
}
