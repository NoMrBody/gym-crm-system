package dao;

import model.Trainee;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TraineeDAOTest {

    private TraineeDAO traineeDAO;

    @BeforeEach
    void setUp() {
        traineeDAO = new TraineeDAO();
        traineeDAO.setTraineeData(new HashMap<>());
    }

    @Test
    void create_assignsIdAndStores() {
        Trainee trainee = new Trainee();
        trainee.setFirstName("Jane");

        Trainee created = traineeDAO.create(trainee);

        assertEquals(1L, created.getUserId());
        assertSame(trainee, traineeDAO.getById(1L));
    }

    @Test
    void getById_returnsStoredTrainee() {
        Trainee trainee = new Trainee();
        traineeDAO.create(trainee);

        assertNotNull(traineeDAO.getById(1L));
        assertNull(traineeDAO.getById(99L));
    }

    @Test
    void update_existingTrainee_replacesAndReturns() {
        Trainee trainee = new Trainee();
        traineeDAO.create(trainee);

        trainee.setFirstName("Updated");
        Trainee result = traineeDAO.update(trainee);

        assertNotNull(result);
        assertEquals("Updated", traineeDAO.getById(1L).getFirstName());
    }

    @Test
    void update_nonExistentTrainee_returnsNull() {
        Trainee trainee = new Trainee();
        trainee.setUserId(42L);

        assertNull(traineeDAO.update(trainee));
    }

    @Test
    void delete_removesTrainee() {
        Trainee trainee = new Trainee();
        traineeDAO.create(trainee);

        traineeDAO.delete(1L);

        assertNull(traineeDAO.getById(1L));
    }

    @Test
    void getAll_returnsAllTrainees() {
        traineeDAO.create(new Trainee());
        traineeDAO.create(new Trainee());

        List<Trainee> all = traineeDAO.getAll();

        assertEquals(2, all.size());
    }

    @Test
    void create_afterPreloadedData_continuesFromMaxKeyWithoutOverwriting() {
        Map<Long, Trainee> preloaded = new HashMap<>();
        Trainee seeded = new Trainee();
        seeded.setUserId(5L);
        preloaded.put(5L, seeded);

        TraineeDAO dao = new TraineeDAO();
        dao.setTraineeData(preloaded);

        Trainee created = dao.create(new Trainee());

        assertEquals(6L, created.getUserId());
        assertSame(seeded, dao.getById(5L));
    }
}
