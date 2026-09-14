package com.gymcrm.workload.jms;

import com.gymcrm.workload.AbstractMongoIntegrationTest;
import com.gymcrm.workload.config.JmsConfig;
import com.gymcrm.workload.dto.TrainerWorkloadRequest;
import com.gymcrm.workload.dto.TrainerWorkloadSummaryResponse;
import com.gymcrm.workload.model.ActionType;
import com.gymcrm.workload.service.TrainerWorkloadService;
import com.gymcrm.workload.exception.TrainerNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class WorkloadMessageListenerTest extends AbstractMongoIntegrationTest {

    @Autowired
    private JmsTemplate jmsTemplate;

    @Autowired
    private TrainerWorkloadService workloadService;

    @Value("${gym.workload.queue}")
    private String queue;

    @Test
    void anAddEventIsAppliedToTheMonthlyTotal() {
        TrainerWorkloadRequest request = new TrainerWorkloadRequest(
                "Jms.Trainer", "Alice", "Cooper", true,
                LocalDate.of(2026, 3, 12), 60, ActionType.ADD);

        jmsTemplate.convertAndSend(queue, request);

        TrainerWorkloadSummaryResponse summary = awaitSummary("Jms.Trainer");
        assertEquals("Jms.Trainer", summary.trainerUsername());
        assertEquals("Alice", summary.trainerFirstName());
        assertEquals(1, summary.years().size());
        assertEquals(2026, summary.years().getFirst().year());
        assertEquals(3, summary.years().getFirst().months().getFirst().month());
        assertEquals(60, summary.years().getFirst().months().getFirst().trainingSummaryDuration());
    }

    @Test
    void theTransactionIdPropertyIsAcceptedOnTheMessage() {
        TrainerWorkloadRequest request = new TrainerWorkloadRequest(
                "Jms.Traced", "Alice", "Cooper", true,
                LocalDate.of(2026, 4, 1), 45, ActionType.ADD);

        jmsTemplate.convertAndSend(queue, request, message -> {
            message.setStringProperty(JmsConfig.TRANSACTION_ID_PROPERTY, "core-transaction-1");
            return message;
        });

        TrainerWorkloadSummaryResponse summary = awaitSummary("Jms.Traced");
        assertEquals(45, summary.years().getFirst().months().getFirst().trainingSummaryDuration());
        assertEquals(4, workloadService.getMonthlyWorkload("Jms.Traced", 2026, 4).month());
    }

    private TrainerWorkloadSummaryResponse awaitSummary(String username) {
        long deadline = System.currentTimeMillis() + 5_000;
        TrainerNotFoundException last = null;
        while (System.currentTimeMillis() < deadline) {
            try {
                return workloadService.getSummary(username);
            } catch (TrainerNotFoundException ex) {
                last = ex;
                try {
                    Thread.sleep(50);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Interrupted while waiting for JMS apply", interrupted);
                }
            }
        }
        throw last != null ? last : new TrainerNotFoundException("No workload recorded for trainer: " + username);
    }

    @Test
    void anInvalidPayloadIsDroppedAndDoesNotCreateATrainer() {
        TrainerWorkloadRequest invalid = new TrainerWorkloadRequest(
                "", "Alice", "Cooper", true,
                LocalDate.of(2026, 3, 12), 60, ActionType.ADD);

        jmsTemplate.convertAndSend(queue, invalid);

        boolean stillMissing = true;
        long deadline = System.currentTimeMillis() + 1_000;
        while (System.currentTimeMillis() < deadline) {
            try {
                workloadService.getSummary("");
                stillMissing = false;
                break;
            } catch (TrainerNotFoundException ignored) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(interrupted);
                }
            }
        }
        assertTrue(stillMissing);
    }
}
