package dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record UpdateTraineeRequest(
        @NotBlank(message = "firstName is required") String firstName,
        @NotBlank(message = "lastName is required") String lastName,
        LocalDate dateOfBirth,
        String address,
        @NotNull(message = "isActive is required") Boolean isActive
) {
}
