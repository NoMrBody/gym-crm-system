package service;

import exception.AuthenticationException;
import metrics.GymMetrics;
import model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import repository.UserRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private GymMetrics gymMetrics;

    @InjectMocks
    private AuthenticationService authenticationService;

    @Test
    void authenticate_validCredentials_returnsUser() {
        User user = new User();
        user.setUsername("Jane.Smith");
        when(userRepository.findByUsernameAndPassword("Jane.Smith", "secret")).thenReturn(Optional.of(user));

        User result = authenticationService.authenticate("Jane.Smith", "secret");

        assertSame(user, result);
        verify(gymMetrics).recordAuthResult(true);
    }

    @Test
    void authenticate_invalidCredentials_throws() {
        when(userRepository.findByUsernameAndPassword("Jane.Smith", "wrong")).thenReturn(Optional.empty());

        assertThrows(AuthenticationException.class,
                () -> authenticationService.authenticate("Jane.Smith", "wrong"));
        verify(gymMetrics).recordAuthResult(false);
    }

    @Test
    void authenticate_nullUsername_throwsWithoutQueryingRepository() {
        assertThrows(AuthenticationException.class,
                () -> authenticationService.authenticate(null, "secret"));
        verifyNoInteractions(userRepository);
        verify(gymMetrics).recordAuthResult(false);
    }

    @Test
    void authenticate_nullPassword_throwsWithoutQueryingRepository() {
        assertThrows(AuthenticationException.class,
                () -> authenticationService.authenticate("Jane.Smith", null));
        verifyNoInteractions(userRepository);
        verify(gymMetrics).recordAuthResult(false);
    }

    @Test
    void changeLogin_validOldPassword_updatesPassword() {
        User user = new User();
        user.setUsername("Jane.Smith");
        user.setPassword("old");
        when(userRepository.findByUsernameAndPassword("Jane.Smith", "old")).thenReturn(Optional.of(user));

        authenticationService.changeLogin("Jane.Smith", "old", "newSecret");

        assertEquals("newSecret", user.getPassword());
        verify(gymMetrics).recordAuthResult(true);
    }

    @Test
    void changeLogin_invalidOldPassword_throwsAndDoesNotChange() {
        when(userRepository.findByUsernameAndPassword("Jane.Smith", "wrong")).thenReturn(Optional.empty());

        assertThrows(AuthenticationException.class,
                () -> authenticationService.changeLogin("Jane.Smith", "wrong", "newSecret"));
        verify(gymMetrics).recordAuthResult(false);
    }
}
