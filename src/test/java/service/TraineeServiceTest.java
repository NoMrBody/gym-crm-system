package service;

import dao.TraineeDAO;
import model.Trainee;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TraineeServiceTest {

    @Mock
    private TraineeDAO traineeDAO;

    @InjectMocks
    private TraineeService traineeService;

    @Test
    void testCreateTrainee_Success() {
        Trainee trainee = new Trainee();
        trainee.setFirstName("Jane");
        trainee.setLastName("Smith");

        // Tell our mocked DAO to return an empty list when checking for existing usernames
        when(traineeDAO.getAll()).thenReturn(new ArrayList<>());
        when(traineeDAO.create(any(Trainee.class))).thenReturn(trainee);

        Trainee created = traineeService.create(trainee);

        // Assertions to verify our specific assignment rules
        assertNotNull(created.getUsername());
        assertEquals("Jane.Smith", created.getUsername());
        assertNotNull(created.getPassword());
        assertEquals(10, created.getPassword().length());
        assertTrue(created.isActive());

        verify(traineeDAO, times(1)).create(trainee);
    }

    @Test
    void testCreateTrainee_DuplicateUsername_AppendsSerial() {
        // Setup an existing user
        Trainee existingTrainee = new Trainee();
        existingTrainee.setUsername("Jane.Smith");
        List<Trainee> existingList = new ArrayList<>();
        existingList.add(existingTrainee);

        // The new user with the same first and last name
        Trainee newTrainee = new Trainee();
        newTrainee.setFirstName("Jane");
        newTrainee.setLastName("Smith");

        when(traineeDAO.getAll()).thenReturn(existingList);
        when(traineeDAO.create(any(Trainee.class))).thenReturn(newTrainee);

        Trainee created = traineeService.create(newTrainee);

        // Verify the logic successfully appended the "1"
        assertEquals("Jane.Smith1", created.getUsername());
    }
}