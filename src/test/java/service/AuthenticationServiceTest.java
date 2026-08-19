package service;

import dto.TokenResponse;
import exception.AuthenticationException;
import exception.UserBlockedException;
import model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import repository.UserRepository;
import security.BruteForceProtector;
import security.JwtService;

import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    private static final UserDetails PRINCIPAL = org.springframework.security.core.userdetails.User
            .withUsername("Jane.Smith")
            .password("{bcrypt}$2a$10$hash")
            .authorities(AuthorityUtils.createAuthorityList("ROLE_USER"))
            .build();

    @Mock
    private UserRepository userRepository;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private BruteForceProtector bruteForceProtector;

    private AuthenticationService authenticationService;

    @BeforeEach
    void setUp() {
        authenticationService = new AuthenticationService();
        authenticationService.setUserRepository(userRepository);
        authenticationService.setAuthenticationManager(authenticationManager);
        authenticationService.setPasswordEncoder(passwordEncoder);
        authenticationService.setJwtService(jwtService);
        authenticationService.setBruteForceProtector(bruteForceProtector);
    }

    private void givenAuthenticationSucceeds() {
        Authentication authenticated = UsernamePasswordAuthenticationToken.authenticated(
                PRINCIPAL, null, PRINCIPAL.getAuthorities());
        when(authenticationManager.authenticate(any())).thenReturn(authenticated);
    }

    @Test
    void login_validCredentials_returnsToken() {
        TokenResponse token = new TokenResponse("token-value", "Bearer", 3600);
        givenAuthenticationSucceeds();
        when(jwtService.issueToken(PRINCIPAL)).thenReturn(token);

        assertSame(token, authenticationService.login("Jane.Smith", "secret"));
    }

    @Test
    void login_invalidCredentials_throwsAuthenticationException() {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(AuthenticationException.class,
                () -> authenticationService.login("Jane.Smith", "wrong"));
        verifyNoInteractions(jwtService);
    }

    @Test
    void login_blockedUser_throwsUserBlockedExceptionWithRemainingTime() {
        when(authenticationManager.authenticate(any())).thenThrow(new LockedException("Account locked"));
        when(bruteForceProtector.remainingBlock("Jane.Smith")).thenReturn(Duration.ofMinutes(4).plusSeconds(30));

        UserBlockedException ex = assertThrows(UserBlockedException.class,
                () -> authenticationService.login("Jane.Smith", "secret"));

        assertEquals("Too many failed login attempts. Try again in 5 minute(s)", ex.getMessage());
    }

    @Test
    void login_nullUsername_throwsWithoutCallingAuthenticationManager() {
        assertThrows(AuthenticationException.class,
                () -> authenticationService.login(null, "secret"));
        verifyNoInteractions(authenticationManager);
    }

    @Test
    void login_nullPassword_throwsWithoutCallingAuthenticationManager() {
        assertThrows(AuthenticationException.class,
                () -> authenticationService.login("Jane.Smith", null));
        verifyNoInteractions(authenticationManager);
    }

    @Test
    void issueTokenFor_delegatesToJwtService() {
        TokenResponse token = new TokenResponse("token-value", "Bearer", 3600);
        when(jwtService.issueToken("Jane.Smith", java.util.List.of("ROLE_USER"))).thenReturn(token);

        assertSame(token, authenticationService.issueTokenFor("Jane.Smith"));
    }

    @Test
    void changeLogin_validOldPassword_storesEncodedPassword() {
        User user = new User();
        user.setUsername("Jane.Smith");
        user.setPassword("{bcrypt}$2a$10$old");
        givenAuthenticationSucceeds();
        when(userRepository.findByUsername("Jane.Smith")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newSecret")).thenReturn("{bcrypt}$2a$10$new");

        authenticationService.changeLogin("Jane.Smith", "old", "newSecret");

        assertEquals("{bcrypt}$2a$10$new", user.getPassword());
    }

    @Test
    void changeLogin_invalidOldPassword_throwsAndDoesNotEncode() {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(AuthenticationException.class,
                () -> authenticationService.changeLogin("Jane.Smith", "wrong", "newSecret"));
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void changeLogin_blankNewPassword_throwsBeforeTouchingTheUser() {
        givenAuthenticationSucceeds();

        assertThrows(exception.ValidationException.class,
                () -> authenticationService.changeLogin("Jane.Smith", "old", "  "));
        verify(userRepository, never()).findByUsername(any());
    }
}
