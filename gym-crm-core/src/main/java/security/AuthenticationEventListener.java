package security;

import metrics.GymMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

/**
 * Feeds Spring Security's authentication outcomes into the brute-force counters and the
 * gym metrics that back the authentication health indicator.
 *
 * <p>Only bad-credentials failures count towards a block. Reacting to lockout failures too
 * would let a blocked user extend their own block by retrying.
 */
@Component
public class AuthenticationEventListener {
    private static final Logger log = LoggerFactory.getLogger(AuthenticationEventListener.class);

    private final BruteForceProtector bruteForceProtector;
    private final GymMetrics gymMetrics;

    public AuthenticationEventListener(BruteForceProtector bruteForceProtector, GymMetrics gymMetrics) {
        this.bruteForceProtector = bruteForceProtector;
        this.gymMetrics = gymMetrics; 
    }

    @EventListener
    public void onSuccess(AuthenticationSuccessEvent event) {
        String username = event.getAuthentication().getName();
        log.debug("Authentication succeeded for user '{}'", username);
        bruteForceProtector.reset(username);
        gymMetrics.recordAuthResult(true);
    }

    @EventListener
    public void onBadCredentials(AuthenticationFailureBadCredentialsEvent event) {
        String username = event.getAuthentication().getName();
        log.warn("Authentication failed for user '{}'", username);
        bruteForceProtector.recordFailure(username);
        gymMetrics.recordAuthResult(false);
    }
}
