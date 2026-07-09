package service;

import dao.UserDAO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock
    private UserDAO userDAO;

    @InjectMocks
    private ProfileService profileService;

    @Test
    void generatesPlainUsername_whenNoConflict() {
        when(userDAO.existsByUsername("Jane.Smith")).thenReturn(false);

        assertEquals("Jane.Smith", profileService.generateUsername("Jane", "Smith"));
    }

    @Test
    void appendsSerial_whenBaseUsernameTaken() {
        when(userDAO.existsByUsername("Jane.Smith")).thenReturn(true);
        when(userDAO.existsByUsername("Jane.Smith1")).thenReturn(false);

        assertEquals("Jane.Smith1", profileService.generateUsername("Jane", "Smith"));
    }

    @Test
    void incrementsSerial_untilFreeUsernameFound() {
        when(userDAO.existsByUsername("Jane.Smith")).thenReturn(true);
        when(userDAO.existsByUsername("Jane.Smith1")).thenReturn(true);
        when(userDAO.existsByUsername("Jane.Smith2")).thenReturn(false);

        assertEquals("Jane.Smith2", profileService.generateUsername("Jane", "Smith"));
    }

    @Test
    void generatesTenCharPassword() {
        assertEquals(10, profileService.generatePassword().length());
    }
}
