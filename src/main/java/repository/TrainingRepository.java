package repository;

import model.Training;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public interface TrainingRepository extends JpaRepository<Training, Long> {

    // CAST is required so Postgres can type null optional params in "(:param IS NULL OR ...)".
    @Query("""
            SELECT t FROM Training t
            WHERE t.trainee.user.username = :traineeUsername
              AND (CAST(:from AS timestamp) IS NULL OR t.trainingDate >= :from)
              AND (CAST(:to AS timestamp) IS NULL OR t.trainingDate <= :to)
              AND (CAST(:trainerUsername AS string) IS NULL OR t.trainer.user.username = :trainerUsername)
              AND (CAST(:typeName AS string) IS NULL OR t.trainingType.trainingTypeName = :typeName)
            ORDER BY t.trainingDate
            """)
    List<Training> searchTraineeTrainings(@Param("traineeUsername") String traineeUsername,
                                          @Param("from") LocalDateTime from,
                                          @Param("to") LocalDateTime to,
                                          @Param("trainerUsername") String trainerUsername,
                                          @Param("typeName") String typeName);

    @Query("""
            SELECT t FROM Training t
            WHERE t.trainer.user.username = :trainerUsername
              AND (CAST(:from AS timestamp) IS NULL OR t.trainingDate >= :from)
              AND (CAST(:to AS timestamp) IS NULL OR t.trainingDate <= :to)
              AND (CAST(:traineeUsername AS string) IS NULL OR t.trainee.user.username = :traineeUsername)
            ORDER BY t.trainingDate
            """)
    List<Training> searchTrainerTrainings(@Param("trainerUsername") String trainerUsername,
                                          @Param("from") LocalDateTime from,
                                          @Param("to") LocalDateTime to,
                                          @Param("traineeUsername") String traineeUsername);

    default List<Training> findTraineeTrainings(String traineeUsername,
                                                LocalDate fromDate,
                                                LocalDate toDate,
                                                String trainerName,
                                                String trainingTypeName) {
        return searchTraineeTrainings(traineeUsername, startOf(fromDate), endOf(toDate),
                blankToNull(trainerName), blankToNull(trainingTypeName));
    }

    default List<Training> findTrainerTrainings(String trainerUsername,
                                                LocalDate fromDate,
                                                LocalDate toDate,
                                                String traineeName) {
        return searchTrainerTrainings(trainerUsername, startOf(fromDate), endOf(toDate),
                blankToNull(traineeName));
    }

    private static LocalDateTime startOf(LocalDate date) {
        return date == null ? null : date.atStartOfDay();
    }

    private static LocalDateTime endOf(LocalDate date) {
        return date == null ? null : date.atTime(LocalTime.MAX);
    }

    private static String blankToNull(String value) {
        return StringUtils.hasText(value) ? value : null;
    }
}
