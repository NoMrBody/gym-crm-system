package health;

import model.TrainingType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;
import repository.TrainingTypeRepository;

import java.util.List;

@Component
public class TrainingTypesHealthIndicator implements HealthIndicator {
    private static final Logger log = LoggerFactory.getLogger(TrainingTypesHealthIndicator.class);

    private final TrainingTypeRepository trainingTypeRepository;

    public TrainingTypesHealthIndicator(TrainingTypeRepository trainingTypeRepository) {
        this.trainingTypeRepository = trainingTypeRepository;
    }

    @Override
    public Health health() {
        List<TrainingType> types = trainingTypeRepository.findAll();
        if (types.isEmpty()) {
            log.warn("Training types health check DOWN: reference table is empty");
            return Health.down()
                    .withDetail("message", "no training types seeded")
                    .withDetail("count", 0)
                    .build();
        }
        List<String> names = types.stream()
                .map(TrainingType::getTrainingTypeName)
                .toList();
        log.debug("Training types health check OK: {}", names);
        return Health.up()
                .withDetail("count", types.size())
                .withDetail("types", names)
                .build();
    }
}
