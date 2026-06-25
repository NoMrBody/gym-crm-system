package service;

import dao.TrainerDAO;
import model.Trainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
public class TrainerService {
    private static final Logger log = LoggerFactory.getLogger(TrainerService.class);

    private TrainerDAO trainerDAO;

    @Autowired
    public void setTrainerDAO(TrainerDAO trainerDAO) {
        this.trainerDAO = trainerDAO;
    }

    public Trainer create(Trainer trainer) {
        log.info("Creating profile for Trainer: {} {}", trainer.getFirstName(), trainer.getLastName());

        trainer.setUsername(generateUsername(trainer.getFirstName(), trainer.getLastName()));
        trainer.setPassword(generatePassword());
        trainer.setActive(true);

        return trainerDAO.create(trainer);
    }

    public Trainer update(Trainer trainer) {
        log.info("Updating Trainer profile: {}", trainer.getUsername());
        return trainerDAO.update(trainer);
    }

    public Trainer getById(Long id) {
        log.info("Fetching Trainer with ID: {}", id);
        return trainerDAO.getById(id);
    }

    private String generateUsername(String firstName, String lastName) {
        String baseUsername = firstName + "." + lastName;
        String finalUsername = baseUsername;
        int serial = 1;

        while (usernameExists(finalUsername)) {
            finalUsername = baseUsername + serial;
            serial++;
        }
        return finalUsername;
    }

    private boolean usernameExists(String username) {
        return trainerDAO.getAll().stream()
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
