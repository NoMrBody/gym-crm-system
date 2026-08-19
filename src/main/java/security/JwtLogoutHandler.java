package security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;

/**
 * Revokes the presented bearer token on logout. Tokens are self-contained, so ending a
 * session early means remembering the token id until it would have expired anyway.
 *
 * <p>{@code LogoutFilter} runs ahead of {@code BearerTokenAuthenticationFilter}, so the
 * security context is still empty here and the token has to be read off the request.
 */
@Component
public class JwtLogoutHandler implements LogoutHandler {
    /** Marks a successful revocation for {@link JwtLogoutSuccessHandler}. */
    static final String REVOKED_ATTRIBUTE = "gym.security.logout.revoked";

    private static final Logger log = LoggerFactory.getLogger(JwtLogoutHandler.class);

    private final TokenDenylist tokenDenylist;
    private final JwtDecoder jwtDecoder;
    private final BearerTokenResolver bearerTokenResolver = new DefaultBearerTokenResolver();

    public JwtLogoutHandler(TokenDenylist tokenDenylist, JwtDecoder jwtDecoder) {
        this.tokenDenylist = tokenDenylist;
        this.jwtDecoder = jwtDecoder;
    }

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        String token = bearerTokenResolver.resolve(request);
        if (token == null) {
            log.debug("Logout called without a bearer token");
            return;
        }

        try {
            Jwt jwt = jwtDecoder.decode(token);
            tokenDenylist.revoke(jwt.getId(), jwt.getExpiresAt());
            request.setAttribute(REVOKED_ATTRIBUTE, Boolean.TRUE);
            log.info("User '{}' logged out", jwt.getSubject());
        } catch (JwtException ex) {
            log.warn("Logout attempted with an unusable bearer token: {}", ex.getMessage());
        }
    }
}
