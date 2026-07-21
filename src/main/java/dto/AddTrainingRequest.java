package dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;

public record AddTrainingRequest(
        @NotBlank(message = "traineeUsername is required") String traineeUsername,
        @NotBlank(message = "trainerUsername is required") String trainerUsername,
        @NotBlank(message = "trainingName is required") String trainingName,
        @NotNull(message = "trainingDate is required") LocalDateTime trainingDate,
        @NotNull(message = "trainingDuration is required") @Positive(message = "trainingDuration must be positive") Integer trainingDuration
) {
}
