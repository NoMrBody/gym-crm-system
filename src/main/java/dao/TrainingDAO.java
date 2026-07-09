package dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import model.Training;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Repository
public class TrainingDAO {
    private static final Logger log = LoggerFactory.getLogger(TrainingDAO.class);

    @PersistenceContext
    private EntityManager entityManager;

    public Training save(Training training) {
        entityManager.persist(training);
        log.info("Persisted new Training with ID: {}", training.getId());
        return training;
    }

    public List<Training> findTraineeTrainings(String traineeUsername,
                                               LocalDate fromDate,
                                               LocalDate toDate,
                                               String trainerName,
                                               String trainingTypeName) {
        StringBuilder jpql = new StringBuilder(
                "SELECT t FROM Training t WHERE t.trainee.user.username = :traineeUsername");
        if (fromDate != null) {
            jpql.append(" AND t.trainingDate >= :fromDate");
        }
        if (toDate != null) {
            jpql.append(" AND t.trainingDate <= :toDate");
        }
        if (StringUtils.hasText(trainerName)) {
            jpql.append(" AND t.trainer.user.username = :trainerName");
        }
        if (StringUtils.hasText(trainingTypeName)) {
            jpql.append(" AND t.trainingType.trainingTypeName = :trainingTypeName");
        }
        jpql.append(" ORDER BY t.trainingDate");

        TypedQuery<Training> query = entityManager.createQuery(jpql.toString(), Training.class);
        query.setParameter("traineeUsername", traineeUsername);
        if (fromDate != null) {
            query.setParameter("fromDate", fromDate.atStartOfDay());
        }
        if (toDate != null) {
            query.setParameter("toDate", toDate.atTime(LocalTime.MAX));
        }
        if (StringUtils.hasText(trainerName)) {
            query.setParameter("trainerName", trainerName);
        }
        if (StringUtils.hasText(trainingTypeName)) {
            query.setParameter("trainingTypeName", trainingTypeName);
        }
        return query.getResultList();
    }

    public List<Training> findTrainerTrainings(String trainerUsername,
                                               LocalDate fromDate,
                                               LocalDate toDate,
                                               String traineeName) {
        StringBuilder jpql = new StringBuilder(
                "SELECT t FROM Training t WHERE t.trainer.user.username = :trainerUsername");
        if (fromDate != null) {
            jpql.append(" AND t.trainingDate >= :fromDate");
        }
        if (toDate != null) {
            jpql.append(" AND t.trainingDate <= :toDate");
        }
        if (StringUtils.hasText(traineeName)) {
            jpql.append(" AND t.trainee.user.username = :traineeName");
        }
        jpql.append(" ORDER BY t.trainingDate");

        TypedQuery<Training> query = entityManager.createQuery(jpql.toString(), Training.class);
        query.setParameter("trainerUsername", trainerUsername);
        if (fromDate != null) {
            query.setParameter("fromDate", fromDate.atStartOfDay());
        }
        if (toDate != null) {
            query.setParameter("toDate", toDate.atTime(LocalTime.MAX));
        }
        if (StringUtils.hasText(traineeName)) {
            query.setParameter("traineeName", traineeName);
        }
        return query.getResultList();
    }
}
