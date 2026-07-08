package service;

import dao.TraineeDAO;
import model.Trainee;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TraineeServiceTest {

    @Mock
    private TraineeDAO traineeDAO;

    @Mock
    private ProfileService profileService;

    @InjectMocks
    private TraineeService traineeService;

    @Test
    void testCreateTrainee_Success() {
        Trainee trainee = new Trainee();
        trainee.setFirstName("Jane");
        trainee.setLastName("Smith");
        when(profileService.generateUsername("Jane", "Smith")).thenReturn("Jane.Smith");
        when(profileService.generatePassword()).thenReturn("ABCDE12345"); // 10 chars
        when(traineeDAO.create(any(Trainee.class))).thenReturn(trainee);
        Trainee created = traineeService.create(trainee);
        assertEquals("Jane.Smith", created.getUsername());
        assertEquals(10, created.getPassword().length());
        assertTrue(created.isActive());
        verify(traineeDAO, times(1)).create(trainee);
    }

    @Test
    void testCreateTrainee_DelegatesUsernameToProfileService() {
        Trainee newTrainee = new Trainee();
        newTrainee.setFirstName("Jane");
        newTrainee.setLastName("Smith");

        when(profileService.generateUsername("Jane", "Smith")).thenReturn("Jane.Smith1");
        when(profileService.generatePassword()).thenReturn("ABCDE12345");
        when(traineeDAO.create(any(Trainee.class))).thenReturn(newTrainee);

        Trainee created = traineeService.create(newTrainee);

        assertEquals("Jane.Smith1", created.getUsername());
    }
}