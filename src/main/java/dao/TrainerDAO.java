package dao;

import model.Trainer;
import storage.InMemoryStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Repository
public class TrainerDAO {
    private static final Logger log = LoggerFactory.getLogger(TrainerDAO.class);
    private final Map<Long, Object> trainerData;
    private Long idCounter = 1L;

    @Autowired
    public TrainerDAO(InMemoryStorage storage) {
        this.trainerData = storage.getStorage().get("TRAINER");
    }

    public Trainer create(Trainer trainer) {
        trainer.setUserId(idCounter++);
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
        return (Trainer) trainerData.get(id);
    }

    public List<Trainer> getAll() {
        List<Trainer> allTrainers = new ArrayList<>();
        for (Object obj : trainerData.values()) {
            allTrainers.add((Trainer) obj);
        }
        return allTrainers;
    }
}
