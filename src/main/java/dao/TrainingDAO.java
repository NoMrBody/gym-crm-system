package dao;

import model.Training;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class TrainingDAO {
    private static final Logger log = LoggerFactory.getLogger(TrainingDAO.class);
    private Map<Long, Training> trainingData;
    private final AtomicLong idCounter = new AtomicLong(0L);

    @Autowired
    public void setTrainingData(@Qualifier("trainingStorage") Map<Long, Training> trainingData) {
        this.trainingData = trainingData;
        this.idCounter.set(
                trainingData.keySet().stream().max(Long::compareTo).orElse(0L)
        );
    }

    public Training create(Training training) {
        training.setId(idCounter.incrementAndGet());
        trainingData.put(training.getId(), training);
        log.info("Saved new Training to storage with ID: {}", training.getId());
        return training;
    }

    public Training getById(Long id) {
        return trainingData.get(id);
    }

    public List<Training> getAll() {
        return new ArrayList<>(trainingData.values());
    }
}
