package util;

import exception.ValidationException;
import model.Trainee;
import model.Trainer;
import model.Training;
import model.User;

public final class ValidationUtils {

    private ValidationUtils() {
    }

    public static void requireNonBlank(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new ValidationException(fieldName + " is required and cannot be blank");
        }
    }

    public static void requireNonNull(Object value, String fieldName) {
        if (value == null) {
            throw new ValidationException(fieldName + " is required and cannot be null");
        }
    }

    public static void validateUser(User user) {
        requireNonNull(user, "user");
        requireNonBlank(user.getFirstName(), "firstName");
        requireNonBlank(user.getLastName(), "lastName");
    }

    public static void validateTrainee(Trainee trainee) {
        requireNonNull(trainee, "trainee");
        validateUser(trainee.getUser());
    }

    public static void validateTrainer(Trainer trainer) {
        requireNonNull(trainer, "trainer");
        validateUser(trainer.getUser());
        requireNonNull(trainer.getSpecialization(), "specialization");
    }

    public static void validateTraining(Training training) {
        requireNonNull(training, "training");
        requireNonNull(training.getTrainee(), "trainee");
        requireNonNull(training.getTrainer(), "trainer");
        requireNonBlank(training.getTrainingName(), "trainingName");
        requireNonNull(training.getTrainingType(), "trainingType");
        requireNonNull(training.getTrainingDate(), "trainingDate");
        requireNonNull(training.getTrainingDuration(), "trainingDuration");
    }
}
