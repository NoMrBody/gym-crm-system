package dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Bearer token issued on login or registration")
public record TokenResponse(
        @Schema(description = "Signed JWT to send as 'Authorization: Bearer <token>'")
        String accessToken,

        @Schema(description = "Authorization scheme", example = "Bearer")
        String tokenType,

        @Schema(description = "Token lifetime in seconds", example = "3600")
        long expiresIn
) {
}
