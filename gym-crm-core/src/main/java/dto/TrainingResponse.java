package dto;

import java.time.LocalDateTime;

public record TrainingResponse(
        String trainingName,
        LocalDateTime trainingDate,
        String trainingType,
        Integer trainingDuration,
        String trainerName,
        String traineeName
) {
}
