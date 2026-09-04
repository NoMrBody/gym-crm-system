package workload;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Forwards committed training changes to trainer-workload-service.
 *
 * <p>Listening on AFTER_COMMIT means the remote service is never told about a training that
 * ended up rolled back. The listener runs on the request thread, so the transaction ID is still
 * in the MDC and travels with the outgoing call.
 */
@Component
public class TrainingChangedEventListener {
    private static final Logger log = LoggerFactory.getLogger(TrainingChangedEventListener.class);

    private final TrainerWorkloadService workloadService;

    public TrainingChangedEventListener(TrainerWorkloadService workloadService) {
        this.workloadService = workloadService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTrainingChanged(TrainingChangedEvent event) {
        log.debug("Handling committed {} event for trainer '{}'",
                event.actionType(), event.workload().trainerUsername());
        workloadService.notifyWorkloadChange(event.workload());
    }
}
