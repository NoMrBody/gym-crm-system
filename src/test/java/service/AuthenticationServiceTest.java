package service;

import dao.UserDAO;
import exception.AuthenticationException;
import model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private UserDAO userDAO;

    @InjectMocks
    private AuthenticationService authenticationService;

    @Test
    void authenticate_validCredentials_returnsUser() {
        User user = new User();
        user.setUsername("Jane.Smith");
        when(userDAO.findByCredentials("Jane.Smith", "secret")).thenReturn(Optional.of(user));

        User result = authenticationService.authenticate("Jane.Smith", "secret");

        assertSame(user, result);
    }

    @Test
    void authenticate_invalidCredentials_throws() {
        when(userDAO.findByCredentials("Jane.Smith", "wrong")).thenReturn(Optional.empty());

        assertThrows(AuthenticationException.class,
                () -> authenticationService.authenticate("Jane.Smith", "wrong"));
    }

    @Test
    void authenticate_nullUsername_throwsWithoutQueryingDao() {
        assertThrows(AuthenticationException.class,
                () -> authenticationService.authenticate(null, "secret"));
        verifyNoInteractions(userDAO);
    }

    @Test
    void authenticate_nullPassword_throwsWithoutQueryingDao() {
        assertThrows(AuthenticationException.class,
                () -> authenticationService.authenticate("Jane.Smith", null));
        verifyNoInteractions(userDAO);
    }
}
