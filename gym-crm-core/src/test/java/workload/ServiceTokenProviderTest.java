package workload;

import dto.TokenResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import security.JwtService;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceTokenProviderTest {

    private static final long TTL_SECONDS = 3600;

    @Mock
    private JwtService jwtService;

    /** A clock the test moves forward by hand. */
    private static class MovableClock extends Clock {
        private Instant now = Instant.parse("2026-03-12T10:00:00Z");

        void advance(Duration amount) {
            now = now.plus(amount);
        }

        @Override
        public Instant instant() {
            return now;
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }
    }

    @Test
    void theTokenIsIssuedOnceAndThenServedFromTheCache() {
        MovableClock clock = new MovableClock();
        when(jwtService.issueToken(ServiceTokenProvider.SERVICE_ACCOUNT,
                List.of(ServiceTokenProvider.SERVICE_ROLE)))
                .thenReturn(new TokenResponse("first-token", "Bearer", TTL_SECONDS));
        ServiceTokenProvider provider = new ServiceTokenProvider(jwtService, clock);

        assertEquals("first-token", provider.currentToken());
        clock.advance(Duration.ofMinutes(30));
        assertEquals("first-token", provider.currentToken());

        verify(jwtService, times(1)).issueToken(ServiceTokenProvider.SERVICE_ACCOUNT,
                List.of(ServiceTokenProvider.SERVICE_ROLE));
    }

    @Test
    void theTokenIsReissuedShortlyBeforeItExpires() {
        MovableClock clock = new MovableClock();
        when(jwtService.issueToken(ServiceTokenProvider.SERVICE_ACCOUNT,
                List.of(ServiceTokenProvider.SERVICE_ROLE)))
                .thenReturn(new TokenResponse("first-token", "Bearer", TTL_SECONDS))
                .thenReturn(new TokenResponse("second-token", "Bearer", TTL_SECONDS));
        ServiceTokenProvider provider = new ServiceTokenProvider(jwtService, clock);

        assertEquals("first-token", provider.currentToken());

        // Inside the last minute of the token's life, so it must not be handed out again.
        clock.advance(Duration.ofSeconds(TTL_SECONDS - 30));

        assertEquals("second-token", provider.currentToken());
        verify(jwtService, times(2)).issueToken(ServiceTokenProvider.SERVICE_ACCOUNT,
                List.of(ServiceTokenProvider.SERVICE_ROLE));
    }
}
