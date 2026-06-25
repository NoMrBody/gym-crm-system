package dao;

import model.Training;
import storage.InMemoryStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Repository
public class TrainingDAO {
    private static final Logger log = LoggerFactory.getLogger(TrainingDAO.class);
    private final Map<Long, Object> trainingData;
    private Long idCounter = 1L;

    @Autowired
    public TrainingDAO(InMemoryStorage storage) {
        this.trainingData = storage.getStorage().get("TRAINING");
    }

    public Training create(Training training) {
        training.setId(idCounter++);
        trainingData.put(training.getId(), training);
        log.info("Saved new Training to storage with ID: {}", training.getId());
        return training;
    }

    public Training getById(Long id) {
        return (Training) trainingData.get(id);
    }

    public List<Training> getAll() {
        List<Training> allTrainings = new ArrayList<>();
        for (Object obj : trainingData.values()) {
            allTrainings.add((Training) obj);
        }
        return allTrainings;
    }
}
