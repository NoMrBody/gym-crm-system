package facade;

import model.Trainee;
import model.Trainer;
import model.Training;
import model.TrainingType;
import model.User;
import service.AuthenticationService;
import service.TraineeService;
import service.TrainerService;
import service.TrainingService;
import service.TrainingTypeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Facade for gym CRM operations. Only {@link #login} and {@link #changeLogin} require password verification; all others use username.
 */
@Component
public class GymFacade {
    private static final Logger log = LoggerFactory.getLogger(GymFacade.class);

    private final TraineeService traineeService;
    private final TrainerService trainerService;
    private final TrainingService trainingService;
    private final TrainingTypeService trainingTypeService;
    private final AuthenticationService authenticationService;

    @Autowired
    public GymFacade(TraineeService traineeService,
                     TrainerService trainerService,
                     TrainingService trainingService,
                     TrainingTypeService trainingTypeService,
                     AuthenticationService authenticationService) {
        this.traineeService = traineeService;
        this.trainerService = trainerService;
        this.trainingService = trainingService;
        this.trainingTypeService = trainingTypeService;
        this.authenticationService = authenticationService;
        log.info("GymFacade initialized with all services.");
    }

    // Create Trainee profile (public registration).
    public Trainee createTrainee(Trainee trainee) {
        return traineeService.create(trainee);
    }

    // Create Trainer profile (public registration).
    public Trainer createTrainer(Trainer trainer) {
        return trainerService.create(trainer);
    }

    // Login: verify username/password matching.
    public User login(String username, String password) {
        return authenticationService.authenticate(username, password);
    }

    // Change login password (verifies old password).
    public void changeLogin(String username, String oldPassword, String newPassword) {
        authenticationService.changeLogin(username, oldPassword, newPassword);
    }

    // Get Trainee profile by username.
    public Trainee getTrainee(String username) {
        return traineeService.getByUsername(username);
    }

    // Get Trainer profile by username.
    public Trainer getTrainer(String username) {
        return trainerService.getByUsername(username);
    }

    // Update Trainee profile.
    public Trainee updateTrainee(String username, Trainee updatedData) {
        return traineeService.update(username, updatedData);
    }

    // Update Trainer profile.
    public Trainer updateTrainer(String username, Trainer updatedData) {
        return trainerService.update(username, updatedData);
    }

    // Activate/De-Activate Trainee (non-idempotent).
    public void setTraineeActive(String username, boolean active) {
        if (active) {
            traineeService.activate(username);
        } else {
            traineeService.deactivate(username);
        }
    }

    // Activate/De-Activate Trainer (non-idempotent).
    public void setTrainerActive(String username, boolean active) {
        if (active) {
            trainerService.activate(username);
        } else {
            trainerService.deactivate(username);
        }
    }

    // Delete Trainee profile by username (hard delete, cascades trainings).
    public void deleteTrainee(String username) {
        traineeService.deleteByUsername(username);
    }

    // Get Trainee trainings list by optional criteria.
    public List<Training> getTraineeTrainings(String username,
                                              LocalDate fromDate,
                                              LocalDate toDate,
                                              String trainerName,
                                              String trainingTypeName) {
        return traineeService.getTrainings(username, fromDate, toDate, trainerName, trainingTypeName);
    }

    // Get Trainer trainings list by optional criteria.
    public List<Training> getTrainerTrainings(String username,
                                              LocalDate fromDate,
                                              LocalDate toDate,
                                              String traineeName) {
        return trainerService.getTrainings(username, fromDate, toDate, traineeName);
    }

    // Add Training.
    public Training addTraining(String traineeUsername,
                                String trainerUsername,
                                String trainingName,
                                LocalDateTime trainingDate,
                                Integer trainingDuration) {
        return trainingService.addTraining(traineeUsername, trainerUsername,
                trainingName, trainingDate, trainingDuration);
    }

    // Get active trainers not yet assigned to the Trainee.
    public List<Trainer> getUnassignedTrainers(String username) {
        return traineeService.getUnassignedTrainers(username);
    }

    // Update the Trainee's trainers list.
    public Trainee updateTraineeTrainers(String username, List<String> trainerUsernames) {
        return traineeService.updateTrainers(username, trainerUsernames);
    }

    // Get the constant list of training types.
    public List<TrainingType> getTrainingTypes() {
        return trainingTypeService.getAll();
    }
}
