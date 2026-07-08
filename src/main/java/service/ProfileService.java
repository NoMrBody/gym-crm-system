package service;

import dao.TraineeDAO;
import dao.TrainerDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

@Service
public class ProfileService {
    private static final Logger log = LoggerFactory.getLogger(ProfileService.class);

    private TraineeDAO traineeDAO;
    private TrainerDAO trainerDAO;

    @Autowired
    public void setTraineeDAO(TraineeDAO traineeDAO) {
        this.traineeDAO = traineeDAO;
    }

    @Autowired
    public void setTrainerDAO(TrainerDAO trainerDAO) {
        this.trainerDAO = trainerDAO;
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
        boolean exists = traineeDAO.getAll().stream().anyMatch(t -> username.equals(t.getUsername()))
                || trainerDAO.getAll().stream().anyMatch(t -> username.equals(t.getUsername()));
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
