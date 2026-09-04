package com.gymcrm.workload.controller;

import com.gymcrm.workload.dto.MonthlyWorkloadResponse;
import com.gymcrm.workload.dto.TrainerWorkloadRequest;
import com.gymcrm.workload.dto.TrainerWorkloadSummaryResponse;
import com.gymcrm.workload.service.TrainerWorkloadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/trainer-workloads")
@Tag(name = "Trainer workload", description = "Monthly training hours per trainer")
public class TrainerWorkloadController {
    private static final Logger log = LoggerFactory.getLogger(TrainerWorkloadController.class);

    private final TrainerWorkloadService workloadService;

    public TrainerWorkloadController(TrainerWorkloadService workloadService) {
        this.workloadService = workloadService;
    }

    @PostMapping
    @Operation(summary = "Report a training session",
            description = "Adds or removes a training session's duration from the trainer's monthly total.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Workload updated"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
            @ApiResponse(responseCode = "403", description = "Caller is not a service account")
    })
    public ResponseEntity<Void> submitWorkload(@Valid @RequestBody TrainerWorkloadRequest request) {
        log.info("Received {} workload event for trainer '{}' on {} ({} minutes)",
                request.actionType(), request.trainerUsername(),
                request.trainingDate(), request.trainingDuration());
        workloadService.apply(request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{username}")
    @Operation(summary = "Get a trainer's monthly summary",
            description = "Returns the training duration totals grouped by year and month.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Summary returned"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
            @ApiResponse(responseCode = "404", description = "No workload recorded for the trainer")
    })
    public ResponseEntity<TrainerWorkloadSummaryResponse> getSummary(@PathVariable String username) {
        log.info("Fetching workload summary for trainer '{}'", username);
        return ResponseEntity.ok(workloadService.getSummary(username));
    }

    @GetMapping("/{username}/years/{year}/months/{month}")
    @Operation(summary = "Get a trainer's workload for one month",
            description = "Returns the summed training duration; zero when the trainer had no trainings that month.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Monthly total returned"),
            @ApiResponse(responseCode = "400", description = "Invalid year or month"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
            @ApiResponse(responseCode = "404", description = "No workload recorded for the trainer")
    })
    public ResponseEntity<MonthlyWorkloadResponse> getMonthlyWorkload(
            @PathVariable String username,
            @PathVariable @Min(1970) @Max(9999) int year,
            @PathVariable @Min(1) @Max(12) int month) {
        log.info("Fetching {}-{} workload for trainer '{}'", year, month, username);
        return ResponseEntity.ok(workloadService.getMonthlyWorkload(username, year, month));
    }
}
