package service;

import dao.TrainerDAO;
import dao.TrainingDAO;
import dao.TrainingTypeDAO;
import exception.ValidationException;
import jakarta.persistence.EntityNotFoundException;
import model.Trainer;
import model.Training;
import model.TrainingType;
import model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import util.ValidationUtils;

import java.time.LocalDate;
import java.util.List;

@Service
public class TrainerService {
    private static final Logger log = LoggerFactory.getLogger(TrainerService.class);

    private TrainerDAO trainerDAO;
    private TrainingDAO trainingDAO;
    private TrainingTypeDAO trainingTypeDAO;
    private ProfileService profileService;
    private AuthenticationService authenticationService;

    @Autowired
    public void setTrainerDAO(TrainerDAO trainerDAO) {
        this.trainerDAO = trainerDAO;
    }

    @Autowired
    public void setTrainingDAO(TrainingDAO trainingDAO) {
        this.trainingDAO = trainingDAO;
    }

    @Autowired
    public void setTrainingTypeDAO(TrainingTypeDAO trainingTypeDAO) {
        this.trainingTypeDAO = trainingTypeDAO;
    }

    @Autowired
    public void setProfileService(ProfileService profileService) {
        this.profileService = profileService;
    }

    @Autowired
    public void setAuthenticationService(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    // #2 Create Trainer profile. Generates username/password, marks the profile active. No authentication required (public registration).
    @Transactional
    public Trainer create(Trainer trainer) {
        ValidationUtils.validateTrainer(trainer);
        trainer.setSpecialization(resolveSpecialization(trainer.getSpecialization()));

        User user = trainer.getUser();
        user.setUsername(profileService.generateUsername(user.getFirstName(), user.getLastName()));
        user.setPassword(profileService.generatePassword());
        user.setActive(true);

        Trainer saved = trainerDAO.save(trainer);
        log.info("Created Trainer profile with username: {}", user.getUsername());
        return saved;
    }

    // #6 Select Trainer profile by username (authenticated)
    @Transactional(readOnly = true)
    public Trainer getByUsername(String username, String password) {
        authenticationService.authenticate(username, password);
        log.debug("Fetching Trainer profile: {}", username);
        return requireTrainer(username);
    }

    // #10 Update Trainer (name, specialization only. activation handled separately)
    @Transactional
    public Trainer update(String username, String password, Trainer updatedData) {
        authenticationService.authenticate(username, password);
        ValidationUtils.validateTrainer(updatedData);

        Trainer existing = requireTrainer(username);
        User existingUser = existing.getUser();
        User newUser = updatedData.getUser();
        existingUser.setFirstName(newUser.getFirstName());
        existingUser.setLastName(newUser.getLastName());
        existing.setSpecialization(resolveSpecialization(updatedData.getSpecialization()));

        Trainer saved = trainerDAO.save(existing);
        log.info("Updated Trainer profile: {}", username);
        return saved;
    }

    // #8 Change Trainer password (requires current password)
    @Transactional
    public void changePassword(String username, String oldPassword, String newPassword) {
        authenticationService.authenticate(username, oldPassword);
        ValidationUtils.requireNonBlank(newPassword, "newPassword");

        Trainer trainer = requireTrainer(username);
        trainer.getUser().setPassword(newPassword);
        trainerDAO.save(trainer);
        log.info("Password changed for Trainer: {}", username);
    }

    // #12 Activate Trainer (non-idempotent: rejects a no-op change)
    @Transactional
    public void activate(String username, String password) {
        authenticationService.authenticate(username, password);
        Trainer trainer = requireTrainer(username);
        if (trainer.getUser().isActive()) {
            throw new ValidationException("Trainer '" + username + "' is already active");
        }
        trainer.getUser().setActive(true);
        trainerDAO.save(trainer);
        log.info("Activated Trainer: {}", username);
    }

    // #12 Deactivate Trainer (non-idempotent: rejects a no-op change)
    @Transactional
    public void deactivate(String username, String password) {
        authenticationService.authenticate(username, password);
        Trainer trainer = requireTrainer(username);
        if (!trainer.getUser().isActive()) {
            throw new ValidationException("Trainer '" + username + "' is already inactive");
        }
        trainer.getUser().setActive(false);
        trainerDAO.save(trainer);
        log.info("Deactivated Trainer: {}", username);
    }

    // #15 Get Trainer trainings list by optional criteria
    @Transactional(readOnly = true)
    public List<Training> getTrainings(String username,
                                       String password,
                                       LocalDate fromDate,
                                       LocalDate toDate,
                                       String traineeName) {
        authenticationService.authenticate(username, password);
        log.debug("Fetching trainings for Trainer: {}", username);
        return trainingDAO.findTrainerTrainings(username, fromDate, toDate, traineeName);
    }

    private TrainingType resolveSpecialization(TrainingType specialization) {
        if (specialization == null) {
            throw new ValidationException("specialization is required and cannot be null");
        }
        if (specialization.getId() != null) {
            return trainingTypeDAO.findById(specialization.getId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "TrainingType not found with id: " + specialization.getId()));
        }
        return trainingTypeDAO.findByName(specialization.getTrainingTypeName())
                .orElseThrow(() -> new EntityNotFoundException(
                        "TrainingType not found with name: " + specialization.getTrainingTypeName()));
    }

    private Trainer requireTrainer(String username) {
        return trainerDAO.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("Trainer not found with username: " + username));
    }
}
