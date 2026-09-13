package com.gymcrm.workload.jms;

import com.gymcrm.workload.config.JmsConfig;
import com.gymcrm.workload.dto.TrainerWorkloadRequest;
import com.gymcrm.workload.service.TrainerWorkloadService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Set;
import java.util.UUID;

/**
 * Applies committed training changes published by gym-crm-core.
 *
 * <p>Invalid payloads are logged and dropped so a poison message is not redelivered forever.
 * Unexpected failures are rethrown so ActiveMQ can retry.
 */
@Component
public class WorkloadMessageListener {
    private static final Logger log = LoggerFactory.getLogger(WorkloadMessageListener.class);

    private static final String TRANSACTION_ID_MDC_KEY = "transactionId";

    private final TrainerWorkloadService workloadService;
    private final Validator validator;

    public WorkloadMessageListener(TrainerWorkloadService workloadService, Validator validator) {
        this.workloadService = workloadService;
        this.validator = validator;
    }

    @JmsListener(destination = "${gym.workload.queue}")
    public void onWorkload(TrainerWorkloadRequest request,
                           @Header(name = JmsConfig.TRANSACTION_ID_PROPERTY, required = false)
                           String transactionId) {
        String resolvedTransactionId = StringUtils.hasText(transactionId)
                ? transactionId
                : UUID.randomUUID().toString();
        MDC.put(TRANSACTION_ID_MDC_KEY, resolvedTransactionId);
        try {
            Set<ConstraintViolation<TrainerWorkloadRequest>> violations = validator.validate(request);
            if (!violations.isEmpty()) {
                log.warn("Dropping invalid workload message: {}",
                        new ConstraintViolationException(violations).getMessage());
                return;
            }
            log.info("Received {} workload event for trainer '{}' on {} ({} minutes)",
                    request.actionType(), request.trainerUsername(),
                    request.trainingDate(), request.trainingDuration());
            workloadService.apply(request);
        } catch (RuntimeException ex) {
            log.error("Failed to apply workload event for trainer '{}': {}",
                    request == null ? "unknown" : request.trainerUsername(), ex.getMessage());
            throw ex;
        } finally {
            MDC.remove(TRANSACTION_ID_MDC_KEY);
        }
    }
}
