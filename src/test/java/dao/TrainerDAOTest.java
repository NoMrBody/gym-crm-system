package dao;

import model.Trainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TrainerDAOTest {

    private TrainerDAO trainerDAO;

    @BeforeEach
    void setUp() {
        trainerDAO = new TrainerDAO();
        trainerDAO.setTrainerData(new HashMap<>());
    }

    @Test
    void create_assignsIdAndStores() {
        Trainer trainer = new Trainer();
        trainer.setFirstName("Alice");

        Trainer created = trainerDAO.create(trainer);

        assertEquals(1L, created.getUserId());
        assertSame(trainer, trainerDAO.getById(1L));
    }

    @Test
    void getById_returnsStoredTrainer() {
        trainerDAO.create(new Trainer());

        assertNotNull(trainerDAO.getById(1L));
        assertNull(trainerDAO.getById(99L));
    }

    @Test
    void update_existingTrainer_replacesAndReturns() {
        Trainer trainer = new Trainer();
        trainerDAO.create(trainer);

        trainer.setSpecialization("Yoga");
        Trainer result = trainerDAO.update(trainer);

        assertNotNull(result);
        assertEquals("Yoga", trainerDAO.getById(1L).getSpecialization());
    }

    @Test
    void update_nonExistentTrainer_returnsNull() {
        Trainer trainer = new Trainer();
        trainer.setUserId(42L);

        assertNull(trainerDAO.update(trainer));
    }

    @Test
    void getAll_returnsAllTrainers() {
        trainerDAO.create(new Trainer());
        trainerDAO.create(new Trainer());

        List<Trainer> all = trainerDAO.getAll();

        assertEquals(2, all.size());
    }

    @Test
    void create_afterPreloadedData_continuesFromMaxKeyWithoutOverwriting() {
        Map<Long, Trainer> preloaded = new HashMap<>();
        Trainer seeded = new Trainer();
        seeded.setUserId(5L);
        preloaded.put(5L, seeded);

        TrainerDAO dao = new TrainerDAO();
        dao.setTrainerData(preloaded);

        Trainer created = dao.create(new Trainer());

        assertEquals(6L, created.getUserId());
        assertSame(seeded, dao.getById(5L));
    }
}
