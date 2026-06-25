package service;

import dao.TrainerDAO;
import model.Trainer;
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
class TrainerServiceTest {

    @Mock
    private TrainerDAO trainerDAO;

    @InjectMocks
    private TrainerService trainerService;

    @Test
    void testCreateTrainer_Success() {
        Trainer trainer = new Trainer();
        trainer.setFirstName("Alice");
        trainer.setLastName("Cooper");
        trainer.setSpecialization("Strength");

        when(trainerDAO.getAll()).thenReturn(new ArrayList<>());
        when(trainerDAO.create(any(Trainer.class))).thenReturn(trainer);

        Trainer created = trainerService.create(trainer);

        assertNotNull(created.getUsername());
        assertEquals("Alice.Cooper", created.getUsername());
        assertNotNull(created.getPassword());
        assertEquals(10, created.getPassword().length());
        assertTrue(created.isActive());

        verify(trainerDAO, times(1)).create(trainer);
    }

    @Test
    void testCreateTrainer_DuplicateUsername_AppendsSerial() {
        Trainer existingTrainer = new Trainer();
        existingTrainer.setUsername("Alice.Cooper");
        List<Trainer> existingList = new ArrayList<>();
        existingList.add(existingTrainer);

        Trainer newTrainer = new Trainer();
        newTrainer.setFirstName("Alice");
        newTrainer.setLastName("Cooper");

        when(trainerDAO.getAll()).thenReturn(existingList);
        when(trainerDAO.create(any(Trainer.class))).thenReturn(newTrainer);

        Trainer created = trainerService.create(newTrainer);

        assertEquals("Alice.Cooper1", created.getUsername());
    }

    @Test
    void testGetById() {
        Trainer trainer = new Trainer();
        trainer.setUserId(1L);
        when(trainerDAO.getById(1L)).thenReturn(trainer);

        Trainer result = trainerService.getById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getUserId());
        verify(trainerDAO, times(1)).getById(1L);
    }

    @Test
    void testUpdateTrainer() {
        Trainer trainer = new Trainer();
        trainer.setUsername("Alice.Cooper");
        when(trainerDAO.update(any(Trainer.class))).thenReturn(trainer);

        Trainer result = trainerService.update(trainer);

        assertNotNull(result);
        assertEquals("Alice.Cooper", result.getUsername());
        verify(trainerDAO, times(1)).update(trainer);
    }
}