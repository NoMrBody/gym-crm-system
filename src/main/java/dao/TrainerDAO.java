package dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import model.Trainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class TrainerDAO {
    private static final Logger log = LoggerFactory.getLogger(TrainerDAO.class);

    @PersistenceContext
    private EntityManager entityManager;

    public Trainer save(Trainer trainer) {
        if (trainer.getId() == null) {
            entityManager.persist(trainer);
            log.info("Persisted new Trainer with ID: {}", trainer.getId());
            return trainer;
        }
        Trainer merged = entityManager.merge(trainer);
        log.info("Merged Trainer with ID: {}", merged.getId());
        return merged;
    }

    public Optional<Trainer> findById(Long id) {
        return Optional.ofNullable(entityManager.find(Trainer.class, id));
    }

    public Optional<Trainer> findByUsername(String username) {
        TypedQuery<Trainer> query = entityManager.createQuery(
                "SELECT t FROM Trainer t WHERE t.user.username = :username", Trainer.class);
        query.setParameter("username", username);
        try {
            return Optional.of(query.getSingleResult());
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    public boolean existsByUsername(String username) {
        Long count = entityManager.createQuery(
                        "SELECT COUNT(t) FROM Trainer t WHERE t.user.username = :username", Long.class)
                .setParameter("username", username)
                .getSingleResult();
        return count > 0;
    }

    public List<Trainer> findAll() {
        return entityManager.createQuery("SELECT t FROM Trainer t", Trainer.class).getResultList();
    }
}
