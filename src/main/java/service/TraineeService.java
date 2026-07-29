package service;

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
import repository.TraineeRepository;
import repository.TrainerRepository;
import repository.TrainingRepository;
import util.ValidationUtils;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class TraineeService {
    private static final Logger log = LoggerFactory.getLogger(TraineeService.class);

    private TraineeRepository traineeRepository;
    private TrainerRepository trainerRepository;
    private TrainingRepository trainingRepository;
    private ProfileService profileService;

    @Autowired
    public void setTraineeRepository(TraineeRepository traineeRepository) {
        this.traineeRepository = traineeRepository;
    }

    @Autowired
    public void setTrainerRepository(TrainerRepository trainerRepository) {
        this.trainerRepository = trainerRepository;
    }

    @Autowired
    public void setTrainingRepository(TrainingRepository trainingRepository) {
        this.trainingRepository = trainingRepository;
    }

    @Autowired
    public void setProfileService(ProfileService profileService) {
        this.profileService = profileService;
    }

    // Create Trainee profile. Generates credentials and activates (public registration).
    @Transactional
    public Trainee create(Trainee trainee) {
        ValidationUtils.validateTrainee(trainee);
        User user = trainee.getUser();
        user.setUsername(profileService.generateUsername(user.getFirstName(), user.getLastName()));
        user.setPassword(profileService.generatePassword());
        user.setActive(true);
        Trainee saved = traineeRepository.save(trainee);
        log.info("Created Trainee profile with username: {}", user.getUsername());
        return saved;
    }

    // Get Trainee profile by username.
    @Transactional(readOnly = true)
    public Trainee getByUsername(String username) {
        log.debug("Fetching Trainee profile: {}", username);
        return requireTrainee(username);
    }

    // Update Trainee (name, birth date, address, active flag; username is immutable).
    @Transactional
    public Trainee update(String username, Trainee updatedData) {
        ValidationUtils.validateTrainee(updatedData);

        Trainee existing = requireTrainee(username);
        User existingUser = existing.getUser();
        User newUser = updatedData.getUser();
        existingUser.setFirstName(newUser.getFirstName());
        existingUser.setLastName(newUser.getLastName());
        existingUser.setActive(newUser.isActive());
        existing.setDateOfBirth(updatedData.getDateOfBirth());
        existing.setAddress(updatedData.getAddress());

        Trainee saved = traineeRepository.save(existing);
        log.info("Updated Trainee profile: {}", username);
        return saved;
    }

    // Activate Trainee (non-idempotent: rejects a no-op change).
    @Transactional
    public void activate(String username) {
        Trainee trainee = requireTrainee(username);
        if (trainee.getUser().isActive()) {
            throw new ValidationException("Trainee '" + username + "' is already active");
        }
        trainee.getUser().setActive(true);
        traineeRepository.save(trainee);
        log.info("Activated Trainee: {}", username);
    }

    // Deactivate Trainee (non-idempotent: rejects a no-op change).
    @Transactional
    public void deactivate(String username) {
        Trainee trainee = requireTrainee(username);
        if (!trainee.getUser().isActive()) {
            throw new ValidationException("Trainee '" + username + "' is already inactive");
        }
        trainee.getUser().setActive(false);
        traineeRepository.save(trainee);
        log.info("Deactivated Trainee: {}", username);
    }

    // Delete Trainee (hard delete; cascades trainings).
    @Transactional
    public void deleteByUsername(String username) {
        traineeRepository.delete(requireTrainee(username));
        log.info("Deleted Trainee profile: {}", username);
    }

    // Get Trainee trainings list by optional criteria.
    @Transactional(readOnly = true)
    public List<Training> getTrainings(String username,
                                       LocalDate fromDate,
                                       LocalDate toDate,
                                       String trainerName,
                                       String trainingTypeName) {
        requireTrainee(username);
        log.debug("Fetching trainings for Trainee: {}", username);
        return trainingRepository.findTraineeTrainings(username, fromDate, toDate, trainerName, trainingTypeName);
    }

    // Get active trainers not yet assigned to the Trainee.
    @Transactional(readOnly = true)
    public List<Trainer> getUnassignedTrainers(String username) {
        requireTrainee(username);
        log.debug("Fetching unassigned trainers for Trainee: {}", username);
        return trainerRepository.findUnassignedFor(username);
    }

    // Update the Trainee's trainers list (replaces the whole set).
    @Transactional
    public Trainee updateTrainers(String username, List<String> trainerUsernames) {
        ValidationUtils.requireNonNull(trainerUsernames, "trainerUsernames");

        Trainee trainee = requireTrainee(username);
        Set<Trainer> trainers = new HashSet<>();
        for (String trainerUsername : trainerUsernames) {
            Trainer trainer = trainerRepository.findByUser_Username(trainerUsername)
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Trainer not found with username: " + trainerUsername));
            trainers.add(trainer);
        }
        trainee.setTrainers(trainers);

        Trainee saved = traineeRepository.save(trainee);
        log.info("Updated trainers list for Trainee '{}' -> {} trainer(s)", username, trainers.size());
        return saved;
    }

    private Trainee requireTrainee(String username) {
        return traineeRepository.findByUser_Username(username)
                .orElseThrow(() -> new EntityNotFoundException("Trainee not found with username: " + username));
    }
}
