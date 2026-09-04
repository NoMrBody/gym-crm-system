package com.gymcrm.workload.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.gymcrm.workload.model.ActionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

/** A single training session planned (ADD) or cancelled (DELETE) for a trainer. */
public record TrainerWorkloadRequest(
        @NotBlank(message = "trainerUsername is required") String trainerUsername,
        @NotBlank(message = "trainerFirstName is required") String trainerFirstName,
        @NotBlank(message = "trainerLastName is required") String trainerLastName,
        @NotNull(message = "isActive is required") @JsonProperty("isActive") Boolean isActive,
        @NotNull(message = "trainingDate is required") LocalDate trainingDate,
        @NotNull(message = "trainingDuration is required")
        @Positive(message = "trainingDuration must be positive") Integer trainingDuration,
        @NotNull(message = "actionType is required") ActionType actionType
) {
}
