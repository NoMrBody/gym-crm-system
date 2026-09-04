package dto;

import jakarta.validation.constraints.NotNull;

public record StatusRequest(
        @NotNull(message = "isActive is required") Boolean isActive
) {
}
