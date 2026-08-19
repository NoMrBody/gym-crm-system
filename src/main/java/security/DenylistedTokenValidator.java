package security;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * Rejects tokens whose id was revoked by logout. Plugging this into the {@code JwtDecoder}
 * means every protected endpoint enforces revocation without extra wiring.
 */
@Component
public class DenylistedTokenValidator implements OAuth2TokenValidator<Jwt> {

    private static final OAuth2Error REVOKED = new OAuth2Error(
            "invalid_token", "The token has been revoked", null);

    private final TokenDenylist tokenDenylist;

    public DenylistedTokenValidator(TokenDenylist tokenDenylist) {
        this.tokenDenylist = tokenDenylist;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        return tokenDenylist.isRevoked(token.getId())
                ? OAuth2TokenValidatorResult.failure(REVOKED)
                : OAuth2TokenValidatorResult.success();
    }
}
