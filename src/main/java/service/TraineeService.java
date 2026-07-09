package service;

import dao.TraineeDAO;
import dao.TrainerDAO;
import dao.TrainingDAO;
import exception.ValidationException;
import jakarta.persistence.EntityNotFoundException;
import model.Trainee;
import model.Trainer;
import model.Training;
import model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import util.ValidationUtils;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class TraineeService {
    private static final Logger log = LoggerFactory.getLogger(TraineeService.class);

    private TraineeDAO traineeDAO;
    private TrainerDAO trainerDAO;
    private TrainingDAO trainingDAO;
    private ProfileService profileService;
    private AuthenticationService authenticationService;

    @Autowired
    public void setTraineeDAO(TraineeDAO traineeDAO) {
        this.traineeDAO = traineeDAO;
    }

    @Autowired
    public void setTrainerDAO(TrainerDAO trainerDAO) {
        this.trainerDAO = trainerDAO;
    }

    @Autowired
    public void setTrainingDAO(TrainingDAO trainingDAO) {
        this.trainingDAO = trainingDAO;
    }

    @Autowired
    public void setProfileService(ProfileService profileService) {
        this.profileService = profileService;
    }

    @Autowired
    public void setAuthenticationService(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    // Create Trainee profile. Generates credentials and activates (public registration).
    @Transactional
    public Trainee create(Trainee trainee) {
        ValidationUtils.validateTrainee(trainee);
        User user = trainee.getUser();
        user.setUsername(profileService.generateUsername(user.getFirstName(), user.getLastName()));
        user.setPassword(profileService.generatePassword());
        user.setActive(true);
        Trainee saved = traineeDAO.save(trainee);
        log.info("Created Trainee profile with username: {}", user.getUsername());
        return saved;
    }

    // #5 Get Trainee profile by username (authenticated)
    @Transactional(readOnly = true)
    public Trainee getByUsername(String username, String password) {
        authenticationService.authenticate(username, password);
        log.debug("Fetching Trainee profile: {}", username);
        return requireTrainee(username);
    }

    // #9 Update Trainee (name, birth date, address only. activation handled separately)
    @Transactional
    public Trainee update(String username, String password, Trainee updatedData) {
        authenticationService.authenticate(username, password);
        ValidationUtils.validateTrainee(updatedData);

        Trainee existing = requireTrainee(username);
        User existingUser = existing.getUser();
        User newUser = updatedData.getUser();
        existingUser.setFirstName(newUser.getFirstName());
        existingUser.setLastName(newUser.getLastName());
        existing.setDateOfBirth(updatedData.getDateOfBirth());
        existing.setAddress(updatedData.getAddress());

        Trainee saved = traineeDAO.save(existing);
        log.info("Updated Trainee profile: {}", username);
        return saved;
    }

    // #7 Change Trainee password (requires current password)
    @Transactional
    public void changePassword(String username, String oldPassword, String newPassword) {
        authenticationService.authenticate(username, oldPassword);
        ValidationUtils.requireNonBlank(newPassword, "newPassword");

        Trainee trainee = requireTrainee(username);
        trainee.getUser().setPassword(newPassword);
        traineeDAO.save(trainee);
        log.info("Password changed for Trainee: {}", username);
    }

    // #11 Activate Trainee (non-idempotent: rejects a no-op change)
    @Transactional
    public void activate(String username, String password) {
        authenticationService.authenticate(username, password);
        Trainee trainee = requireTrainee(username);
        if (trainee.getUser().isActive()) {
            throw new ValidationException("Trainee '" + username + "' is already active");
        }
        trainee.getUser().setActive(true);
        traineeDAO.save(trainee);
        log.info("Activated Trainee: {}", username);
    }

    // #11 Deactivate Trainee (non-idempotent: rejects a no-op change)
    @Transactional
    public void deactivate(String username, String password) {
        authenticationService.authenticate(username, password);
        Trainee trainee = requireTrainee(username);
        if (!trainee.getUser().isActive()) {
            throw new ValidationException("Trainee '" + username + "' is already inactive");
        }
        trainee.getUser().setActive(false);
        traineeDAO.save(trainee);
        log.info("Deactivated Trainee: {}", username);
    }

    // #13 Delete Trainee (hard delete; cascades trainings)
    @Transactional
    public void deleteByUsername(String username, String password) {
        authenticationService.authenticate(username, password);
        traineeDAO.deleteByUsername(username);
        log.info("Deleted Trainee profile: {}", username);
    }

    // #14 Get Trainee trainings list by optional criteria
    @Transactional(readOnly = true)
    public List<Training> getTrainings(String username,
                                       String password,
                                       LocalDate fromDate,
                                       LocalDate toDate,
                                       String trainerName,
                                       String trainingTypeName) {
        authenticationService.authenticate(username, password);
        log.debug("Fetching trainings for Trainee: {}", username);
        return trainingDAO.findTraineeTrainings(username, fromDate, toDate, trainerName, trainingTypeName);
    }

    // #17 Get active trainers not yet assigned to the Trainee
    @Transactional(readOnly = true)
    public List<Trainer> getUnassignedTrainers(String username, String password) {
        authenticationService.authenticate(username, password);
        log.debug("Fetching unassigned trainers for Trainee: {}", username);
        return traineeDAO.findUnassignedTrainers(username);
    }

    // #18 Update the Trainee's trainers list (replaces the whole set)
    @Transactional
    public Trainee updateTrainers(String username, String password, List<String> trainerUsernames) {
        authenticationService.authenticate(username, password);
        ValidationUtils.requireNonNull(trainerUsernames, "trainerUsernames");

        Trainee trainee = requireTrainee(username);
        Set<Trainer> trainers = new HashSet<>();
        for (String trainerUsername : trainerUsernames) {
            Trainer trainer = trainerDAO.findByUsername(trainerUsername)
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Trainer not found with username: " + trainerUsername));
            trainers.add(trainer);
        }
        trainee.setTrainers(trainers);

        Trainee saved = traineeDAO.save(trainee);
        log.info("Updated trainers list for Trainee '{}' -> {} trainer(s)", username, trainers.size());
        return saved;
    }

    private Trainee requireTrainee(String username) {
        return traineeDAO.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("Trainee not found with username: " + username));
    }
}
