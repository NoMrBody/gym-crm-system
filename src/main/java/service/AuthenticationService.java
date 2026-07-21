package service;

import dao.UserDAO;
import exception.AuthenticationException;
import model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import util.ValidationUtils;

@Service
public class AuthenticationService {
    private static final Logger log = LoggerFactory.getLogger(AuthenticationService.class);

    private UserDAO userDAO;

    @Autowired
    public void setUserDAO(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    @Transactional(readOnly = true)
    public User authenticate(String username, String password) {
        log.debug("Authenticating user: {}", username);
        if (username == null || password == null) {
            throw new AuthenticationException("Username and password are required");
        }
        return userDAO.findByCredentials(username, password)
                .orElseThrow(() -> {
                    log.warn("Authentication failed for username: {}", username);
                    return new AuthenticationException("Invalid username or password");
                });
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
