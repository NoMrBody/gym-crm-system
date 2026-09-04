package dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;

public record TrainerRegistrationRequest(
        @NotBlank(message = "firstName is required") String firstName,
        @NotBlank(message = "lastName is required") String lastName,
        @NotNull(message = "specializationId is required") Long specializationId
) {
}
