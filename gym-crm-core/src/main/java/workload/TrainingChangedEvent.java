package workload;

import model.Training;

/**
 * Published when a training is created or removed. Carries a snapshot rather than the entity,
 * because the listener runs after the transaction committed and the row may already be gone.
 */
public record TrainingChangedEvent(ActionType actionType, TrainerWorkloadRequest workload) {

    public static TrainingChangedEvent of(ActionType actionType, Training training) {
        return new TrainingChangedEvent(actionType, TrainerWorkloadRequest.from(training, actionType));
    }
}
