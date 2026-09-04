package workload;

import dto.TokenResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import security.JwtService;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Supplies the bearer token gym-crm-core authenticates with when it calls
 * trainer-workload-service. The token is signed with the secret both services share, so no
 * round trip to an authorization server is needed; it is cached until shortly before it expires.
 */
@Component
public class ServiceTokenProvider {
    private static final Logger log = LoggerFactory.getLogger(ServiceTokenProvider.class);

    /** Subject of the service token; trainer-workload-service logs it as the caller. */
    public static final String SERVICE_ACCOUNT = "gym-crm-core";

    /** Authority that trainer-workload-service requires for reporting a workload. */
    public static final String SERVICE_ROLE = "ROLE_SERVICE";

    /** Renew this far ahead of expiry, so a token never expires mid-flight. */
    private static final Duration RENEW_BEFORE_EXPIRY = Duration.ofSeconds(60);

    private final JwtService jwtService;
    private final Clock clock;

    private String token;
    private Instant renewAt;

    @Autowired
    public ServiceTokenProvider(JwtService jwtService) {
        this(jwtService, Clock.systemUTC());
    }

    ServiceTokenProvider(JwtService jwtService, Clock clock) {
        this.jwtService = jwtService;
        this.clock = clock;
    }

    public synchronized String currentToken() {
        Instant now = clock.instant();
        if (token == null || now.isAfter(renewAt)) {
            TokenResponse issued = jwtService.issueToken(SERVICE_ACCOUNT, List.of(SERVICE_ROLE));
            token = issued.accessToken();
            renewAt = now.plusSeconds(issued.expiresIn()).minus(RENEW_BEFORE_EXPIRY);
            log.debug("Issued a new service token for '{}', renewing after {}", SERVICE_ACCOUNT, renewAt);
        }
        return token;
    }
}
