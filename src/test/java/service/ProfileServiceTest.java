package service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import repository.UserRepository;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ProfileService profileService;

    @Test
    void generatesPlainUsername_whenNoConflict() {
        when(userRepository.existsByUsername("Jane.Smith")).thenReturn(false);

        assertEquals("Jane.Smith", profileService.generateUsername("Jane", "Smith"));
    }

    @Test
    void appendsSerial_whenBaseUsernameTaken() {
        when(userRepository.existsByUsername("Jane.Smith")).thenReturn(true);
        when(userRepository.existsByUsername("Jane.Smith1")).thenReturn(false);

        assertEquals("Jane.Smith1", profileService.generateUsername("Jane", "Smith"));
    }

    @Test
    void incrementsSerial_untilFreeUsernameFound() {
        when(userRepository.existsByUsername("Jane.Smith")).thenReturn(true);
        when(userRepository.existsByUsername("Jane.Smith1")).thenReturn(true);
        when(userRepository.existsByUsername("Jane.Smith2")).thenReturn(false);

        assertEquals("Jane.Smith2", profileService.generateUsername("Jane", "Smith"));
    }

    @Test
    void generatesTenCharPassword() {
        assertEquals(10, profileService.generatePassword().length());
    }
}
