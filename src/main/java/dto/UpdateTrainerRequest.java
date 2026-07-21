package dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateTrainerRequest(
        @NotBlank(message = "firstName is required") String firstName,
        @NotBlank(message = "lastName is required") String lastName,
        @NotNull(message = "isActive is required") Boolean isActive
) {
}
