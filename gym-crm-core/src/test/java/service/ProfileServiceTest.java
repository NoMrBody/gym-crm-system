package service;

import model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import repository.UserRepository;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    private ProfileService profileService;

    @BeforeEach
    void setUp() {
        profileService = new ProfileService();
        profileService.setUserRepository(userRepository);
        profileService.setPasswordEncoder(passwordEncoder);
    }

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

    @Test
    void assignCredentials_storesHashAndReturnsRawPassword() {
        User user = new User();
        user.setFirstName("Jane");
        user.setLastName("Smith");
        when(userRepository.existsByUsername("Jane.Smith")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("{bcrypt}$2a$10$hash");

        String rawPassword = profileService.assignCredentials(user);

        assertEquals("Jane.Smith", user.getUsername());
        assertEquals(10, rawPassword.length());
        assertEquals("{bcrypt}$2a$10$hash", user.getPassword());
        verify(passwordEncoder).encode(rawPassword);
    }
}
