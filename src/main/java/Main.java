import config.SpringConfig;
import facade.GymFacade;
import lombok.extern.slf4j.Slf4j;
import model.Trainee;
import model.Trainer;
import model.Training;
import model.TrainingType;
import model.User;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.time.LocalDateTime;
import java.time.Month;
import java.time.LocalDate;
import java.util.List;

/**
 * Small end-to-end smoke scenario that exercises the facade against a live PostgreSQL instance.
 * Requires a reachable database (see application.properties); the unit tests themselves are DB-free.
 */
@Slf4j
public class Main {

    public static void main(String[] args) {
        log.info("Starting Gym CRM Application...");

        try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext(SpringConfig.class)) {

            GymFacade facade = context.getBean(GymFacade.class);

            Trainee trainee = facade.createTrainee(newTrainee("John", "Doe",
                    LocalDate.of(2005, Month.JANUARY, 1), "123 Main St"));
            String traineeUsername = trainee.getUser().getUsername();
            String traineePassword = trainee.getUser().getPassword();
            log.info("Created Trainee: {} / {}", traineeUsername, traineePassword);

            Trainer trainer = facade.createTrainer(newTrainer("Anna", "Smith", "Yoga"));
            String trainerUsername = trainer.getUser().getUsername();
            String trainerPassword = trainer.getUser().getPassword();
            log.info("Created Trainer: {} / {}", trainerUsername, trainerPassword);

            Training training = facade.addTraining(trainerUsername, trainerPassword,
                    traineeUsername, trainerUsername, "Morning Yoga",
                    LocalDateTime.now(), 60);
            log.info("Added Training: {}", training.getTrainingName());

            List<Training> traineeTrainings = facade.getTraineeTrainings(
                    traineeUsername, traineePassword, null, null, null, null);
            log.info("Trainee has {} training(s)", traineeTrainings.size());

            List<Trainer> unassigned = facade.getUnassignedTrainers(traineeUsername, traineePassword);
            log.info("Trainee has {} unassigned trainer(s)", unassigned.size());
        }
    }

    private static Trainee newTrainee(String firstName, String lastName,
                                      LocalDate dateOfBirth, String address) {
        User user = new User();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        Trainee trainee = new Trainee();
        trainee.setUser(user);
        trainee.setDateOfBirth(dateOfBirth);
        trainee.setAddress(address);
        return trainee;
    }

    private static Trainer newTrainer(String firstName, String lastName, String specializationName) {
        User user = new User();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        TrainingType specialization = new TrainingType();
        specialization.setTrainingTypeName(specializationName);
        Trainer trainer = new Trainer();
        trainer.setUser(user);
        trainer.setSpecialization(specialization);
        return trainer;
    }
}
