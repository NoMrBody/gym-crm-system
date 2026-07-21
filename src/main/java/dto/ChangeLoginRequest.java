package dto;

import jakarta.validation.constraints.NotBlank;

public record ChangeLoginRequest(
        @NotBlank(message = "username is required") String username,
        @NotBlank(message = "oldPassword is required") String oldPassword,
        @NotBlank(message = "newPassword is required") String newPassword
) {
}
