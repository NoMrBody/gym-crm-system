package dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import model.TrainingType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class TrainingTypeDAO {
    private static final Logger log = LoggerFactory.getLogger(TrainingTypeDAO.class);

    // EntityManager for database operations
    @PersistenceContext
    private EntityManager entityManager;

    public List<TrainingType> findAll() {
        return entityManager.createQuery("SELECT t FROM TrainingType t", TrainingType.class).getResultList();
    }

    public Optional<TrainingType> findById(Long id) {
        return Optional.ofNullable(entityManager.find(TrainingType.class, id));
    }

    public Optional<TrainingType> findByName(String trainingTypeName) {
        TypedQuery<TrainingType> query = entityManager.createQuery(
                "SELECT t FROM TrainingType t WHERE t.trainingTypeName = :name", TrainingType.class);
        query.setParameter("name", trainingTypeName);
        try {
            return Optional.of(query.getSingleResult());
        } catch (NoResultException e) {
            log.debug("No TrainingType found with name: {}", trainingTypeName);
            return Optional.empty();
        }
    }
}
