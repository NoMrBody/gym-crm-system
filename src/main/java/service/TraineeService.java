package service;

import dao.TraineeDAO;
import model.Trainee;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
public class TraineeService {
    private static final Logger log = LoggerFactory.getLogger(TraineeService.class);

    private TraineeDAO traineeDAO;

    @Autowired
    public void setTraineeDAO(TraineeDAO traineeDAO) {
        this.traineeDAO = traineeDAO;
    }

    public Trainee create(Trainee trainee) {
        log.info("Creating profile for Trainee: {} {}", trainee.getFirstName(), trainee.getLastName());

        trainee.setUsername(generateUsername(trainee.getFirstName(), trainee.getLastName()));
        trainee.setPassword(generatePassword());
        trainee.setActive(true);

        return traineeDAO.create(trainee);
    }

    public Trainee update(Trainee trainee) {
        log.info("Updating Trainee profile: {}", trainee.getUsername());
        return traineeDAO.update(trainee);
    }

    public void delete(Long id) {
        log.info("Deleting Trainee profile with ID: {}", id);
        traineeDAO.delete(id);
    }

    public Trainee getById(Long id) {
        log.info("Fetching Trainee with ID: {}", id);
        return traineeDAO.getById(id);
    }

    // helper methods

    private String generateUsername(String firstName, String lastName) {
        String baseUsername = firstName + "." + lastName;
        String finalUsername = baseUsername;
        int serial = 1;

        // Check against existing trainees to prevent duplicate usernames
        while (usernameExists(finalUsername)) {
            finalUsername = baseUsername + serial;
            serial++;
        }
        return finalUsername;
    }

    private boolean usernameExists(String username) {
        return traineeDAO.getAll().stream()
                .anyMatch(t -> username.equals(t.getUsername()));
    }

    private String generatePassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder password = new StringBuilder(10);
        Random random = new Random();
        for (int i = 0; i < 10; i++) {
            password.append(chars.charAt(random.nextInt(chars.length())));
        }
        return password.toString();
    }
}
