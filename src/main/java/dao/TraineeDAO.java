package dao;

import model.Trainee;
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
public class TraineeDAO {
    private static final Logger log = LoggerFactory.getLogger(TraineeDAO.class);
    private Map<Long, Trainee> traineeData;
    private final AtomicLong idCounter = new AtomicLong(0);


    @Autowired
    public void setTraineeData(@Qualifier("traineeStorage") Map<Long, Trainee> traineeData) {
        this.traineeData = traineeData;
        this.idCounter.set(
                traineeData.keySet().stream().max(Long::compareTo).orElse(0L)
        );
    }

    public Trainee create(Trainee trainee) {
        trainee.setUserId(idCounter.incrementAndGet());
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
        return traineeData.get(id);
    }

    public List<Trainee> getAll() {
        return new ArrayList<>(traineeData.values());
    }
}