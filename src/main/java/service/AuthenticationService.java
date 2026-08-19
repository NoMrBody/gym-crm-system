package service;

import dto.TokenResponse;
import exception.AuthenticationException;
import exception.UserBlockedException;
import model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import repository.UserRepository;
import security.BruteForceProtector;
import security.GymUserDetailsService;
import security.JwtService;
import util.ValidationUtils;

import java.time.Duration;
import java.util.List;

/**
 * Verifies credentials through Spring Security and mints the bearer tokens that the rest
 * of the API expects. Failed attempts and lockouts are handled by the authentication
 * manager, which publishes events consumed by {@link BruteForceProtector}.
 */
@Service
public class AuthenticationService {
    private static final Logger log = LoggerFactory.getLogger(AuthenticationService.class);

    private UserRepository userRepository;
    private AuthenticationManager authenticationManager;
    private PasswordEncoder passwordEncoder;
    private JwtService jwtService;
    private BruteForceProtector bruteForceProtector;

    @Autowired
    public void setUserRepository(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Autowired
    public void setAuthenticationManager(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    @Autowired
    public void setPasswordEncoder(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Autowired
    public void setJwtService(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Autowired
    public void setBruteForceProtector(BruteForceProtector bruteForceProtector) {
        this.bruteForceProtector = bruteForceProtector;
    }

    // Verify credentials and issue a bearer token.
    public TokenResponse login(String username, String password) {
        log.debug("Login attempt for user: {}", username);
        Authentication authentication = authenticate(username, password);
        return jwtService.issueToken((UserDetails) authentication.getPrincipal());
    }

    // Issue a token for an already trusted username, used right after registration.
    public TokenResponse issueTokenFor(String username) {
        return jwtService.issueToken(username, List.of(GymUserDetailsService.DEFAULT_ROLE));
    }

    // Change the login password after verifying the current one.
    @Transactional
    public void changeLogin(String username, String oldPassword, String newPassword) {
        authenticate(username, oldPassword);
        ValidationUtils.requireNonBlank(newPassword, "newPassword");

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AuthenticationException("Invalid username or password"));
        user.setPassword(passwordEncoder.encode(newPassword));
        log.info("Password changed for user: {}", username);
    }

    private Authentication authenticate(String username, String password) {
        if (username == null || password == null) {
            throw new AuthenticationException("Username and password are required");
        }
        try {
            return authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password));
        } catch (LockedException ex) {
            Duration remaining = bruteForceProtector.remainingBlock(username);
            long minutes = (remaining.toSeconds() + 59) / 60;
            throw new UserBlockedException("Too many failed login attempts. Try again in "
                    + Math.max(1, minutes) + " minute(s)");
        } catch (org.springframework.security.core.AuthenticationException ex) {
            throw new AuthenticationException("Invalid username or password");
        }
    }
}
