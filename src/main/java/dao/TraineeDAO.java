package dao;

import model.Trainee;
import storage.InMemoryStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Repository
public class TraineeDAO {
    private static final Logger log = LoggerFactory.getLogger(TraineeDAO.class);
    private final Map<Long, Object> traineeData;
    private Long idCounter = 1L; // Simple auto-increment for in-memory map

    @Autowired
    public TraineeDAO(InMemoryStorage storage) {
        this.traineeData = storage.getStorage().get("TRAINEE");
    }

    public Trainee create(Trainee trainee) {
        trainee.setUserId(idCounter++);
        traineeData.put(trainee.getUserId(), trainee);
        log.info("Saved new Trainee to storage with ID: {}", trainee.getUserId());
        return trainee;
    }

    public Trainee update(Trainee trainee) {
        if (traineeData.containsKey(trainee.getUserId())) {
            traineeData.put(trainee.getUserId(), trainee);
            log.info("Updated Trainee in storage with ID: {}", trainee.getUserId());
            return trainee;
        }
        log.warn("Attempted to update non-existent Trainee with ID: {}", trainee.getUserId());
        return null;
    }

    public void delete(Long id) {
        traineeData.remove(id);
        log.info("Deleted Trainee from storage with ID: {}", id);
    }

    public Trainee getById(Long id) {
        return (Trainee) traineeData.get(id);
    }

    public List<Trainee> getAll() {
        List<Trainee> allTrainees = new ArrayList<>();
        for (Object obj : traineeData.values()) {
            allTrainees.add((Trainee) obj);
        }
        return allTrainees;
    }
}