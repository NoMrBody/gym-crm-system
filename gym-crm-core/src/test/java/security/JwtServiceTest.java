package security;

import dto.TokenResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    private static final String ISSUER = "gym-crm-test";
    private static final SecretKey KEY = new SecretKeySpec(
            "test-secret-that-is-long-enough-for-hs256".getBytes(StandardCharsets.UTF_8), "HmacSHA256");

    private final TokenDenylist denylist = new TokenDenylist();

    private JwtEncoder encoder;
    private JwtDecoder decoder;
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        encoder = NimbusJwtEncoder.withSecretKey(KEY).algorithm(MacAlgorithm.HS256).build();

        NimbusJwtDecoder nimbusDecoder = NimbusJwtDecoder.withSecretKey(KEY)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        OAuth2TokenValidator<Jwt> validator = JwtValidators.createDefaultWithValidators(
                new JwtIssuerValidator(ISSUER), new DenylistedTokenValidator(denylist));
        nimbusDecoder.setJwtValidator(validator);
        decoder = nimbusDecoder;

        jwtService = new JwtService(encoder, ISSUER, Duration.ofHours(1));
    }

    private static UserDetails principal(String username) {
        return org.springframework.security.core.userdetails.User
                .withUsername(username)
                .password("{bcrypt}$2a$10$hash")
                .authorities(AuthorityUtils.createAuthorityList("ROLE_USER"))
                .build();
    }

    @Test
    void issuedTokenDecodesBackToTheSameSubject() {
        TokenResponse response = jwtService.issueToken(principal("Jane.Smith"));

        Jwt decoded = decoder.decode(response.accessToken());

        assertEquals("Jane.Smith", decoded.getSubject());
        assertEquals(ISSUER, decoded.getClaimAsString("iss"));
        assertEquals(List.of("ROLE_USER"), decoded.getClaimAsStringList(JwtService.ROLES_CLAIM));
        assertNotNull(decoded.getId());
    }

    @Test
    void tokenResponseDescribesTheBearerScheme() {
        TokenResponse response = jwtService.issueToken("Jane.Smith", List.of("ROLE_USER"));

        assertEquals("Bearer", response.tokenType());
        assertEquals(3600, response.expiresIn());
    }

    @Test
    void expiryMatchesTheConfiguredTtl() {
        TokenResponse response = jwtService.issueToken("Jane.Smith", List.of("ROLE_USER"));

        Jwt decoded = decoder.decode(response.accessToken());

        Duration lifetime = Duration.between(decoded.getIssuedAt(), decoded.getExpiresAt());
        assertEquals(Duration.ofHours(1), lifetime);
        assertTrue(decoded.getExpiresAt().isAfter(Instant.now()));
    }

    @Test
    void everyTokenGetsItsOwnId() {
        Jwt first = decoder.decode(jwtService.issueToken("Jane.Smith", List.of("ROLE_USER")).accessToken());
        Jwt second = decoder.decode(jwtService.issueToken("Jane.Smith", List.of("ROLE_USER")).accessToken());

        assertNotEquals(first.getId(), second.getId());
    }

    @Test
    void expiredTokenIsRejected() {
        // Built directly because JwtService only ever issues tokens with a future expiry.
        // The default validators allow 60s of clock skew, so this expires well before that.
        Instant expiredAt = Instant.now().minus(Duration.ofHours(1));
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .subject("Jane.Smith")
                .issuedAt(expiredAt.minus(Duration.ofHours(1)))
                .expiresAt(expiredAt)
                .id(UUID.randomUUID().toString())
                .build();
        String token = encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();

        assertThrows(JwtValidationException.class, () -> decoder.decode(token));
    }

    @Test
    void tokenFromAnotherIssuerIsRejected() {
        JwtService foreign = new JwtService(encoder, "someone-else", Duration.ofHours(1));
        String token = foreign.issueToken("Jane.Smith", List.of("ROLE_USER")).accessToken();

        assertThrows(JwtValidationException.class, () -> decoder.decode(token));
    }

    @Test
    void revokedTokenIsRejected() {
        String token = jwtService.issueToken("Jane.Smith", List.of("ROLE_USER")).accessToken();
        Jwt decoded = decoder.decode(token);

        denylist.revoke(decoded.getId(), decoded.getExpiresAt());

        assertThrows(JwtValidationException.class, () -> decoder.decode(token));
    }
}
