package service;

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
import repository.TrainerRepository;
import repository.TrainingRepository;
import repository.TrainingTypeRepository;
import util.ValidationUtils;

import java.time.LocalDate;
import java.util.List;

@Service
public class TrainerService {
    private static final Logger log = LoggerFactory.getLogger(TrainerService.class);

    private TrainerRepository trainerRepository;
    private TrainingRepository trainingRepository;
    private TrainingTypeRepository trainingTypeRepository;
    private ProfileService profileService;

    @Autowired
    public void setTrainerRepository(TrainerRepository trainerRepository) {
        this.trainerRepository = trainerRepository;
    }

    @Autowired
    public void setTrainingRepository(TrainingRepository trainingRepository) {
        this.trainingRepository = trainingRepository;
    }

    @Autowired
    public void setTrainingTypeRepository(TrainingTypeRepository trainingTypeRepository) {
        this.trainingTypeRepository = trainingTypeRepository;
    }

    @Autowired
    public void setProfileService(ProfileService profileService) {
        this.profileService = profileService;
    }

    // Create Trainer profile. Generates username/password, marks the profile active (public registration).
    @Transactional
    public RegistrationResult<Trainer> create(Trainer trainer) {
        ValidationUtils.validateTrainer(trainer);
        trainer.setSpecialization(resolveSpecialization(trainer.getSpecialization()));

        User user = trainer.getUser();
        String rawPassword = profileService.assignCredentials(user);
        user.setActive(true);

        Trainer saved = trainerRepository.save(trainer);
        log.info("Created Trainer profile with username: {}", user.getUsername());
        return new RegistrationResult<>(saved, rawPassword);
    }

    // Get Trainer profile by username.
    @Transactional(readOnly = true)
    public Trainer getByUsername(String username) {
        log.debug("Fetching Trainer profile: {}", username);
        return requireTrainer(username);
    }

    // Update Trainer (name and active flag; specialization is read-only, username is immutable).
    @Transactional
    public Trainer update(String username, Trainer updatedData) {
        ValidationUtils.requireNonNull(updatedData, "trainer");
        ValidationUtils.validateUser(updatedData.getUser());

        Trainer existing = requireTrainer(username);
        User existingUser = existing.getUser();
        User newUser = updatedData.getUser();
        existingUser.setFirstName(newUser.getFirstName());
        existingUser.setLastName(newUser.getLastName());
        existingUser.setActive(newUser.isActive());

        Trainer saved = trainerRepository.save(existing);
        log.info("Updated Trainer profile: {}", username);
        return saved;
    }

    // Activate Trainer (non-idempotent: rejects a no-op change).
    @Transactional
    public void activate(String username) {
        Trainer trainer = requireTrainer(username);
        if (trainer.getUser().isActive()) {
            throw new ValidationException("Trainer '" + username + "' is already active");
        }
        trainer.getUser().setActive(true);
        trainerRepository.save(trainer);
        log.info("Activated Trainer: {}", username);
    }

    // Deactivate Trainer (non-idempotent: rejects a no-op change).
    @Transactional
    public void deactivate(String username) {
        Trainer trainer = requireTrainer(username);
        if (!trainer.getUser().isActive()) {
            throw new ValidationException("Trainer '" + username + "' is already inactive");
        }
        trainer.getUser().setActive(false);
        trainerRepository.save(trainer);
        log.info("Deactivated Trainer: {}", username);
    }

    // Get Trainer trainings list by optional criteria.
    @Transactional(readOnly = true)
    public List<Training> getTrainings(String username,
                                       LocalDate fromDate,
                                       LocalDate toDate,
                                       String traineeName) {
        requireTrainer(username);
        log.debug("Fetching trainings for Trainer: {}", username);
        return trainingRepository.findTrainerTrainings(username, fromDate, toDate, traineeName);
    }

    private TrainingType resolveSpecialization(TrainingType specialization) {
        if (specialization == null) {
            throw new ValidationException("specialization is required and cannot be null");
        }
        if (specialization.getId() != null) {
            return trainingTypeRepository.findById(specialization.getId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "TrainingType not found with id: " + specialization.getId()));
        }
        return trainingTypeRepository.findByTrainingTypeName(specialization.getTrainingTypeName())
                .orElseThrow(() -> new EntityNotFoundException(
                        "TrainingType not found with name: " + specialization.getTrainingTypeName()));
    }

    private Trainer requireTrainer(String username) {
        return trainerRepository.findByUser_Username(username)
                .orElseThrow(() -> new EntityNotFoundException("Trainer not found with username: " + username));
    }
}
