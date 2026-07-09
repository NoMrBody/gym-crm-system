package facade;

import model.Trainee;
import model.Trainer;
import model.Training;
import model.User;
import service.AuthenticationService;
import service.TraineeService;
import service.TrainerService;
import service.TrainingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Single entry point exposing all 18 gym CRM operations. Operations that mutate or read
 * protected data require the caller's credentials, which are forwarded to the services for
 * authentication.
 */
@Component
public class GymFacade {
    private static final Logger log = LoggerFactory.getLogger(GymFacade.class);

    private final TraineeService traineeService;
    private final TrainerService trainerService;
    private final TrainingService trainingService;
    private final AuthenticationService authenticationService;

    @Autowired
    public GymFacade(TraineeService traineeService,
                     TrainerService trainerService,
                     TrainingService trainingService,
                     AuthenticationService authenticationService) {
        this.traineeService = traineeService;
        this.trainerService = trainerService;
        this.trainingService = trainingService;
        this.authenticationService = authenticationService;
        log.info("GymFacade initialized with all services.");
    }

    // 1. Create Trainee profile (public registration).
    public Trainee createTrainee(Trainee trainee) {
        return traineeService.create(trainee);
    }

    // 2. Create Trainer profile (public registration).
    public Trainer createTrainer(Trainer trainer) {
        return trainerService.create(trainer);
    }

    // 3. Trainee username/password matching (login).
    public User loginTrainee(String username, String password) {
        return authenticationService.authenticate(username, password);
    }

    // 4. Trainer username/password matching (login).
    public User loginTrainer(String username, String password) {
        return authenticationService.authenticate(username, password);
    }

    // 5. Select Trainee profile by username.
    public Trainee getTrainee(String username, String password) {
        return traineeService.getByUsername(username, password);
    }

    // 6. Select Trainer profile by username.
    public Trainer getTrainer(String username, String password) {
        return trainerService.getByUsername(username, password);
    }

    // 7. Trainee password change.
    public void changeTraineePassword(String username, String oldPassword, String newPassword) {
        traineeService.changePassword(username, oldPassword, newPassword);
    }

    // 8. Trainer password change.
    public void changeTrainerPassword(String username, String oldPassword, String newPassword) {
        trainerService.changePassword(username, oldPassword, newPassword);
    }

    // 9. Update Trainee profile.
    public Trainee updateTrainee(String username, String password, Trainee updatedData) {
        return traineeService.update(username, password, updatedData);
    }

    // 10. Update Trainer profile.
    public Trainer updateTrainer(String username, String password, Trainer updatedData) {
        return trainerService.update(username, password, updatedData);
    }

    // 11. Activate/De-Activate Trainee.
    public void activateTrainee(String username, String password) {
        traineeService.activate(username, password);
    }

    public void deactivateTrainee(String username, String password) {
        traineeService.deactivate(username, password);
    }

    // 12. Activate/De-Activate Trainer.
    public void activateTrainer(String username, String password) {
        trainerService.activate(username, password);
    }

    public void deactivateTrainer(String username, String password) {
        trainerService.deactivate(username, password);
    }

    // 13. Delete Trainee profile by username (hard delete, cascades trainings).
    public void deleteTrainee(String username, String password) {
        traineeService.deleteByUsername(username, password);
    }

    // 14. Get Trainee trainings list by optional criteria.
    public List<Training> getTraineeTrainings(String username,
                                              String password,
                                              LocalDate fromDate,
                                              LocalDate toDate,
                                              String trainerName,
                                              String trainingTypeName) {
        return traineeService.getTrainings(username, password, fromDate, toDate, trainerName, trainingTypeName);
    }

    // 15. Get Trainer trainings list by optional criteria.
    public List<Training> getTrainerTrainings(String username,
                                              String password,
                                              LocalDate fromDate,
                                              LocalDate toDate,
                                              String traineeName) {
        return trainerService.getTrainings(username, password, fromDate, toDate, traineeName);
    }

    // 16. Add Training.
    public Training addTraining(String username,
                                String password,
                                String traineeUsername,
                                String trainerUsername,
                                String trainingName,
                                LocalDateTime trainingDate,
                                Integer trainingDuration) {
        return trainingService.addTraining(username, password, traineeUsername, trainerUsername,
                trainingName, trainingDate, trainingDuration);
    }

    // 17. Get active trainers not yet assigned to the Trainee.
    public List<Trainer> getUnassignedTrainers(String username, String password) {
        return traineeService.getUnassignedTrainers(username, password);
    }

    // 18. Update the Trainee's trainers list.
    public Trainee updateTraineeTrainers(String username, String password, List<String> trainerUsernames) {
        return traineeService.updateTrainers(username, password, trainerUsernames);
    }
}
