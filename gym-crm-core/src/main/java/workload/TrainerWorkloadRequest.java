package workload;

import com.fasterxml.jackson.annotation.JsonProperty;
import model.Training;
import model.User;

import java.time.LocalDate;

/**
 * The payload trainer-workload-service expects for a single training session. Kept in sync
 * with that service's own request record by hand: the two services share no code.
 */
public record TrainerWorkloadRequest(
        String trainerUsername,
        String trainerFirstName,
        String trainerLastName,
        @JsonProperty("isActive") boolean isActive,
        LocalDate trainingDate,
        Integer trainingDuration,
        ActionType actionType
) {

    /** Snapshots the trainer details and the session, so the event survives the entity being deleted. */
    public static TrainerWorkloadRequest from(Training training, ActionType actionType) {
        User trainer = training.getTrainer().getUser();
        return new TrainerWorkloadRequest(
                trainer.getUsername(),
                trainer.getFirstName(),
                trainer.getLastName(),
                trainer.isActive(),
                training.getTrainingDate().toLocalDate(),
                training.getTrainingDuration(),
                actionType);
    }
}
