package service;

import dao.UserDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

@Service
public class ProfileService {
    private static final Logger log = LoggerFactory.getLogger(ProfileService.class);

    private UserDAO userDAO;

    @Autowired
    public void setUserDAO(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    protected String generateUsername(String firstName, String lastName) {
        String baseUsername = firstName + "." + lastName;
        String finalUsername = baseUsername;
        int serial = 1;

        while (usernameExists(finalUsername)) {
            finalUsername = baseUsername + serial;
            serial++;
        }

        if (!finalUsername.equals(baseUsername)) {
            log.debug("Base username '{}' was taken, generated unique username '{}'", baseUsername, finalUsername);
        }
        log.debug("Generated username: {}", finalUsername);
        return finalUsername;
    }

    protected boolean usernameExists(String username) {
        boolean exists = userDAO.existsByUsername(username);
        log.debug("Checking if username '{}' exists: {}", username, exists);
        return exists;
    }

    protected String generatePassword() {
        log.debug("Generating random password");
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder password = new StringBuilder(10);
        SecureRandom random = new SecureRandom();
        for (int i = 0; i < 10; i++) {
            password.append(chars.charAt(random.nextInt(chars.length())));
        }
        return password.toString();
    }
}
