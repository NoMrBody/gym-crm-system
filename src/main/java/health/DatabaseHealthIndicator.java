package health;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;
import repository.TraineeRepository;
import repository.TrainerRepository;
import repository.TrainingRepository;
import repository.UserRepository;

@Component
public class DatabaseHealthIndicator implements HealthIndicator {
    private static final Logger log = LoggerFactory.getLogger(DatabaseHealthIndicator.class);

    private final UserRepository userRepository;
    private final TraineeRepository traineeRepository;
    private final TrainerRepository trainerRepository;
    private final TrainingRepository trainingRepository;

    public DatabaseHealthIndicator(UserRepository userRepository,
                                   TraineeRepository traineeRepository,
                                   TrainerRepository trainerRepository,
                                   TrainingRepository trainingRepository) {
        this.userRepository = userRepository;
        this.traineeRepository = traineeRepository;
        this.trainerRepository = trainerRepository;
        this.trainingRepository = trainingRepository;
    }

    @Override
    public Health health() {
        try {
            long users = userRepository.count();
            long trainees = traineeRepository.count();
            long trainers = trainerRepository.count();
            long trainings = trainingRepository.count();
            log.debug("Database health check OK: users={}, trainees={}, trainers={}, trainings={}",
                    users, trainees, trainers, trainings);
            return Health.up()
                    .withDetail("users", users)
                    .withDetail("trainees", trainees)
                    .withDetail("trainers", trainers)
                    .withDetail("trainings", trainings)
                    .build();
        } catch (Exception ex) {
            log.warn("Database health check failed: {}", ex.getMessage());
            return Health.down(ex).build();
        }
    }
}
