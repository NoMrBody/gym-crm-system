package dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import model.Trainee;
import model.Trainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class TraineeDAO {
    private static final Logger log = LoggerFactory.getLogger(TraineeDAO.class);

    @PersistenceContext
    private EntityManager entityManager;

    public Trainee save(Trainee trainee) {
        if (trainee.getId() == null) {
            entityManager.persist(trainee);
            log.info("Persisted new Trainee with ID: {}", trainee.getId());
            return trainee;
        }
        Trainee merged = entityManager.merge(trainee);
        log.info("Merged Trainee with ID: {}", merged.getId());
        return merged;
    }

    public Optional<Trainee> findById(Long id) {
        return Optional.ofNullable(entityManager.find(Trainee.class, id));
    }

    public Optional<Trainee> findByUsername(String username) {
        TypedQuery<Trainee> query = entityManager.createQuery(
                "SELECT t FROM Trainee t WHERE t.user.username = :username", Trainee.class);
        query.setParameter("username", username);
        try {
            return Optional.of(query.getSingleResult());
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    public boolean existsByUsername(String username) {
        Long count = entityManager.createQuery(
                        "SELECT COUNT(t) FROM Trainee t WHERE t.user.username = :username", Long.class)
                .setParameter("username", username)
                .getSingleResult();
        return count > 0;
    }

    public void deleteByUsername(String username) {
        findByUsername(username).ifPresentOrElse(
                trainee -> {
                    entityManager.remove(trainee);
                    log.info("Deleted Trainee with username: {}", username);
                },
                () -> log.warn("Attempted to delete non-existent Trainee with username: {}", username)
        );
    }

    public List<Trainee> findAll() {
        return entityManager.createQuery("SELECT t FROM Trainee t", Trainee.class).getResultList();
    }

    public List<Trainer> findUnassignedTrainers(String traineeUsername) {
        return entityManager.createQuery(
                        "SELECT tr FROM Trainer tr WHERE tr.user.isActive = true AND tr NOT IN " +
                                "(SELECT assigned FROM Trainee te JOIN te.trainers assigned " +
                                "WHERE te.user.username = :username)", Trainer.class)
                .setParameter("username", traineeUsername)
                .getResultList();
    }
}
