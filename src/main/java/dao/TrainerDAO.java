package dao;

import model.Trainer;
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
public class TrainerDAO {
    private static final Logger log = LoggerFactory.getLogger(TrainerDAO.class);
    private Map<Long, Trainer> trainerData;
    private final AtomicLong idCounter = new AtomicLong(0L);

    @Autowired
    public void setTrainerData(@Qualifier("trainerStorage") Map<Long, Trainer> trainerData){
        this.trainerData = trainerData;
        this.idCounter.set(
                trainerData.keySet().stream().max(Long::compareTo).orElse(0L)
        );
    }

    public Trainer create(Trainer trainer) {
        trainer.setUserId(idCounter.incrementAndGet());
        trainerData.put(trainer.getUserId(), trainer);
        log.info("Saved new Trainer to storage with ID: {}", trainer.getUserId());
        return trainer;
    }

    public Trainer update(Trainer trainer) {
        if (trainerData.containsKey(trainer.getUserId())) {
            trainerData.put(trainer.getUserId(), trainer);
            log.info("Updated Trainer in storage with ID: {}", trainer.getUserId());
            return trainer;
        }
        log.warn("Attempted to update non-existent Trainer with ID: {}", trainer.getUserId());
        return null;
    }

    public Trainer getById(Long id) {
        return trainerData.get(id);
    }

    public List<Trainer> getAll() {
        return new ArrayList<>(trainerData.values());
    }
}
