package bdd;

import com.gymcrm.workload.dto.TrainerWorkloadRequest;
import com.gymcrm.workload.exception.TrainerNotFoundException;
import com.gymcrm.workload.model.ActionType;
import com.gymcrm.workload.service.TrainerWorkloadService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class WorkloadMessagingSupport {

    private final JmsTemplate jmsTemplate;
    private final TrainerWorkloadService workloadService;
    private final String queue;

    public WorkloadMessagingSupport(JmsTemplate jmsTemplate,
                                    TrainerWorkloadService workloadService,
                                    @Value("${gym.workload.queue}") String queue) {
        this.jmsTemplate = jmsTemplate;
        this.workloadService = workloadService;
        this.queue = queue;
    }

    public void send(String username, String firstName, String lastName, String date, int duration, String action) {
        TrainerWorkloadRequest request = new TrainerWorkloadRequest(
                username, firstName, lastName, true, LocalDate.parse(date), duration, ActionType.valueOf(action));
        jmsTemplate.convertAndSend(queue, request);
    }

    public void sendInvalid(String date, int duration) {
        TrainerWorkloadRequest invalid = new TrainerWorkloadRequest(
                "", "Alice", "Cooper", true, LocalDate.parse(date), duration, ActionType.ADD);
        jmsTemplate.convertAndSend(queue, invalid);
    }

    public boolean awaitTrainer(String username, long timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            try {
                workloadService.getSummary(username);
                return true;
            } catch (TrainerNotFoundException ignored) {
                sleep(50);
            }
        }
        return false;
    }

    public void awaitQuietPeriod(long timeoutMs) {
        sleep(timeoutMs);
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(interrupted);
        }
    }
}
