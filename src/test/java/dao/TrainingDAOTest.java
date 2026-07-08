package dao;

import model.Training;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TrainingDAOTest {

    private TrainingDAO trainingDAO;

    @BeforeEach
    void setUp() {
        trainingDAO = new TrainingDAO();
        trainingDAO.setTrainingData(new HashMap<>());
    }

    @Test
    void create_assignsIdAndStores() {
        Training training = new Training();
        training.setTrainingName("Morning Yoga");

        Training created = trainingDAO.create(training);

        assertEquals(1L, created.getId());
        assertSame(training, trainingDAO.getById(1L));
    }

    @Test
    void getById_returnsStoredTraining() {
        trainingDAO.create(new Training());

        assertNotNull(trainingDAO.getById(1L));
        assertNull(trainingDAO.getById(99L));
    }

    @Test
    void getAll_returnsAllTrainings() {
        trainingDAO.create(new Training());
        trainingDAO.create(new Training());

        List<Training> all = trainingDAO.getAll();

        assertEquals(2, all.size());
    }

    @Test
    void create_afterPreloadedData_continuesFromMaxKeyWithoutOverwriting() {
        Map<Long, Training> preloaded = new HashMap<>();
        Training seeded = new Training();
        seeded.setId(5L);
        preloaded.put(5L, seeded);

        TrainingDAO dao = new TrainingDAO();
        dao.setTrainingData(preloaded);

        Training created = dao.create(new Training());

        assertEquals(6L, created.getId());
        assertSame(seeded, dao.getById(5L));
    }
}
