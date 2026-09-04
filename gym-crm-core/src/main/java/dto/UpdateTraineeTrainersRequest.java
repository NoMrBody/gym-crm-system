package dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record UpdateTraineeTrainersRequest(
        @NotEmpty(message = "trainers list is required") List<String> trainerUsernames
) {
}
