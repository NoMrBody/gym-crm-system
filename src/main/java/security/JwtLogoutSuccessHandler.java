package security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Completes logout with 204, or 401 when no usable token was presented. Logout runs before
 * the authorization filter, so this is where the endpoint enforces authentication.
 */
@Component
public class JwtLogoutSuccessHandler implements LogoutSuccessHandler {

    private final SecurityErrorResponder responder;

    public JwtLogoutSuccessHandler(SecurityErrorResponder responder) {
        this.responder = responder;
    }

    @Override
    public void onLogoutSuccess(HttpServletRequest request,
                                HttpServletResponse response,
                                Authentication authentication) throws IOException {
        if (Boolean.TRUE.equals(request.getAttribute(JwtLogoutHandler.REVOKED_ATTRIBUTE))) {
            response.setStatus(HttpStatus.NO_CONTENT.value());
            return;
        }
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer realm=\"gym-crm\"");
        responder.write(response, HttpStatus.UNAUTHORIZED, "Authentication required");
    }
}
