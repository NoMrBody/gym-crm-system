package dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Generated credentials plus a bearer token for the new profile")
public record CredentialsResponse(
        String username,

        @Schema(description = "Generated password, shown only once at registration")
        String password,

        @Schema(description = "Signed JWT to send as 'Authorization: Bearer <token>'")
        String accessToken,

        @Schema(description = "Authorization scheme", example = "Bearer")
        String tokenType,

        @Schema(description = "Token lifetime in seconds", example = "3600")
        long expiresIn
) {
}
