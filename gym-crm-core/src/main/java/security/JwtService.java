package security;

import dto.TokenResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Issues the signed bearer tokens returned by registration and login.
 */
@Service
public class JwtService {
    public static final String TOKEN_TYPE = "Bearer";
    public static final String ROLES_CLAIM = "roles";

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    private final JwtEncoder jwtEncoder;
    private final String issuer;
    private final Duration ttl;

    public JwtService(JwtEncoder jwtEncoder,
                      @Value("${gym.security.jwt.issuer}") String issuer,
                      @Value("${gym.security.jwt.ttl}") Duration ttl) {
        this.jwtEncoder = jwtEncoder;
        this.issuer = issuer;
        this.ttl = ttl;
    }

    public TokenResponse issueToken(UserDetails user) {
        return issueToken(user.getUsername(), user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList());
    }

    public TokenResponse issueToken(String username, List<String> roles) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(ttl);

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(username)
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .id(UUID.randomUUID().toString())
                .claim(ROLES_CLAIM, roles)
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        Jwt jwt = jwtEncoder.encode(JwtEncoderParameters.from(header, claims));

        log.debug("Issued token {} for user '{}', expiring at {}", jwt.getId(), username, expiresAt);
        return new TokenResponse(jwt.getTokenValue(), TOKEN_TYPE, ttl.toSeconds());
    }
}
