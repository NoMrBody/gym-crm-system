package config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import security.DenylistedTokenValidator;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/** HMAC JWTs: the same secret signs and verifies tokens; do not hardcode outside dev. */
@Configuration
public class JwtConfig {

    /** HS256 requires a key of at least 256 bits. */
    private static final int MIN_SECRET_BYTES = 32;

    private final SecretKey secretKey;
    private final String issuer;

    public JwtConfig(@Value("${gym.security.jwt.secret}") String secret,
                     @Value("${gym.security.jwt.issuer}") String issuer) {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "gym.security.jwt.secret must be at least " + MIN_SECRET_BYTES
                            + " bytes for HS256, but was " + keyBytes.length);
        }
        this.secretKey = new SecretKeySpec(keyBytes, "HmacSHA256");
        this.issuer = issuer;
    }

    @Bean
    public JwtEncoder jwtEncoder() {
        return NimbusJwtEncoder.withSecretKey(secretKey)
                .algorithm(MacAlgorithm.HS256)
                .build();
    }

    @Bean
    public JwtDecoder jwtDecoder(DenylistedTokenValidator denylistedTokenValidator) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();

        OAuth2TokenValidator<Jwt> validator = JwtValidators.createDefaultWithValidators(
                new JwtIssuerValidator(issuer), denylistedTokenValidator);
        decoder.setJwtValidator(validator);
        return decoder;
    }
}
