package security;

import dto.ErrorResponse;
import exception.AuthenticationException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import service.AuthenticationService;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

/**
 * Enforces HTTP Basic auth for all endpoints except registration, login,
 * actuator, and OpenAPI docs. Runs after {@code TransactionLoggingFilter}.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class AuthenticationFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(AuthenticationFilter.class);
    private static final String USER_MDC_KEY = "user";
    private static final String BASIC_PREFIX = "Basic ";

    private final AuthenticationService authenticationService;
    private final JsonMapper jsonMapper;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    private final List<WhitelistRule> whitelist = List.of(
            new WhitelistRule("POST", "/api/v1/trainees"),
            new WhitelistRule("POST", "/api/v1/trainers"),
            new WhitelistRule("GET", "/api/v1/login"),
            new WhitelistRule("PUT", "/api/v1/login"),
            new WhitelistRule(null, "/actuator/**"),
            new WhitelistRule(null, "/swagger-ui/**"),
            new WhitelistRule(null, "/swagger-ui.html"),
            new WhitelistRule(null, "/v3/api-docs/**"),
            new WhitelistRule(null, "/v3/api-docs")
    );

    public AuthenticationFilter(AuthenticationService authenticationService, JsonMapper jsonMapper) {
        this.authenticationService = authenticationService;
        this.jsonMapper = jsonMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getRequestURI();
        return whitelist.stream().anyMatch(rule -> rule.matches(method, path, pathMatcher));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.regionMatches(true, 0, BASIC_PREFIX, 0, BASIC_PREFIX.length())) {
            log.warn("Missing or non-Basic Authorization header for {} {}",
                    request.getMethod(), request.getRequestURI());
            writeUnauthorized(response, "Authentication required");
            return;
        }

        String username;
        String password;
        try {
            String decoded = new String(
                    Base64.getDecoder().decode(header.substring(BASIC_PREFIX.length()).trim()),
                    StandardCharsets.UTF_8);
            String[] parts = decoded.split(":", 2);
            if (parts.length != 2 || parts[0].isBlank()) {
                writeUnauthorized(response, "Invalid Authorization header");
                return;
            }
            username = parts[0];
            password = parts[1];
        } catch (IllegalArgumentException ex) {
            log.warn("Malformed Basic credentials for {} {}", request.getMethod(), request.getRequestURI());
            writeUnauthorized(response, "Invalid Authorization header");
            return;
        }

        try {
            authenticationService.authenticate(username, password);
            MDC.put(USER_MDC_KEY, username);
            filterChain.doFilter(request, response);
        } catch (AuthenticationException ex) {
            log.warn("Authentication failed for user '{}': {}", username, ex.getMessage());
            writeUnauthorized(response, ex.getMessage());
        } finally {
            MDC.remove(USER_MDC_KEY);
        }
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"gym-crm\"");
        ErrorResponse body = new ErrorResponse(
                LocalDateTime.now(),
                HttpServletResponse.SC_UNAUTHORIZED,
                "Unauthorized",
                message,
                null,
                MDC.get("transactionId"));
        jsonMapper.writeValue(response.getOutputStream(), body);
    }

    private record WhitelistRule(String method, String pattern) {
        boolean matches(String requestMethod, String path, AntPathMatcher matcher) {
            boolean methodOk = method == null || method.equalsIgnoreCase(requestMethod);
            return methodOk && matcher.match(pattern, path);
        }
    }
}
