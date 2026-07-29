package service;

import exception.AuthenticationException;
import metrics.GymMetrics;
import model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import repository.UserRepository;
import util.ValidationUtils;

import java.util.Optional;

@Service
public class AuthenticationService {
    private static final Logger log = LoggerFactory.getLogger(AuthenticationService.class);

    private UserRepository userRepository;
    private GymMetrics gymMetrics;

    @Autowired
    public void setUserRepository(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Autowired
    public void setGymMetrics(GymMetrics gymMetrics) {
        this.gymMetrics = gymMetrics;
    }

    @Transactional(readOnly = true)
    public User authenticate(String username, String password) {
        log.debug("Authenticating user: {}", username);
        if (username == null || password == null) {
            gymMetrics.recordAuthResult(false);
            throw new AuthenticationException("Username and password are required");
        }
        Optional<User> user = userRepository.findByUsernameAndPassword(username, password);
        if (user.isEmpty()) {
            log.warn("Authentication failed for username: {}", username);
            gymMetrics.recordAuthResult(false);
            throw new AuthenticationException("Invalid username or password");
        }
        gymMetrics.recordAuthResult(true);
        return user.get();
    }

    // Change the login password after verifying the current one.
    @Transactional
    public void changeLogin(String username, String oldPassword, String newPassword) {
        User user = authenticate(username, oldPassword);
        ValidationUtils.requireNonBlank(newPassword, "newPassword");
        user.setPassword(newPassword);
        log.info("Password changed for user: {}", username);
    }
}
