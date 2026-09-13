package com.gymcrm.workload.jms;

import com.gymcrm.workload.dto.TrainerWorkloadRequest;
import com.gymcrm.workload.model.ActionType;
import com.gymcrm.workload.service.TrainerWorkloadService;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WorkloadMessageListenerUnitTest {

    @Mock
    private TrainerWorkloadService workloadService;

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void onWorkload_reusesTheIncomingTransactionIdWhileApplying() {
        WorkloadMessageListener listener = new WorkloadMessageListener(workloadService, validator);
        doAnswer(invocation -> {
            assertEquals("core-transaction-1", MDC.get("transactionId"));
            return null;
        }).when(workloadService).apply(any());

        listener.onWorkload(request("Alice.Cooper"), "core-transaction-1");

        verify(workloadService).apply(any());
    }

    @Test
    void onWorkload_generatesATransactionIdWhenTheHeaderIsMissing() {
        WorkloadMessageListener listener = new WorkloadMessageListener(workloadService, validator);
        doAnswer(invocation -> {
            assertNotNull(MDC.get("transactionId"));
            return null;
        }).when(workloadService).apply(any());

        listener.onWorkload(request("Alice.Cooper"), null);

        verify(workloadService).apply(any());
    }

    @Test
    void onWorkload_dropsAnInvalidPayload() {
        WorkloadMessageListener listener = new WorkloadMessageListener(workloadService, validator);

        listener.onWorkload(new TrainerWorkloadRequest(
                "", "Alice", "Cooper", true, LocalDate.of(2026, 3, 12), 60, ActionType.ADD),
                "tx");

        verify(workloadService, never()).apply(any());
    }

    private static TrainerWorkloadRequest request(String username) {
        return new TrainerWorkloadRequest(username, "Alice", "Cooper", true,
                LocalDate.of(2026, 3, 12), 60, ActionType.ADD);
    }
}
