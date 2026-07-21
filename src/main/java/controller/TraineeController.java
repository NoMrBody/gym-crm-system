package controller;

import dto.CredentialsResponse;
import dto.StatusRequest;
import dto.TraineeProfileResponse;
import dto.TraineeRegistrationRequest;
import dto.TrainerBrief;
import dto.TrainingResponse;
import dto.UpdateTraineeRequest;
import dto.UpdateTraineeTrainersRequest;
import facade.GymFacade;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import mapper.DtoMapper;
import model.Trainee;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/trainees")
@Tag(name = "Trainee", description = "Trainee registration and profile management")
public class TraineeController {
    private static final Logger log = LoggerFactory.getLogger(TraineeController.class);

    private final GymFacade gymFacade;
    private final DtoMapper mapper;

    public TraineeController(GymFacade gymFacade, DtoMapper mapper) {
        this.gymFacade = gymFacade;
        this.mapper = mapper;
    }

    @PostMapping
    @Operation(summary = "Register a trainee", description = "Creates a trainee profile and returns generated credentials.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Trainee created"),
            @ApiResponse(responseCode = "400", description = "Validation error")
    })
    public ResponseEntity<CredentialsResponse> register(@Valid @RequestBody TraineeRegistrationRequest request) {
        log.info("Registering trainee: {} {}", request.firstName(), request.lastName());
        Trainee saved = gymFacade.createTrainee(mapper.toTraineeEntity(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toCredentials(saved.getUser()));
    }

    @GetMapping("/{username}")
    @Operation(summary = "Get trainee profile")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile found"),
            @ApiResponse(responseCode = "404", description = "Trainee not found")
    })
    public ResponseEntity<TraineeProfileResponse> getProfile(@PathVariable String username) {
        log.info("Fetching trainee profile: {}", username);
        return ResponseEntity.ok(mapper.toTraineeProfile(gymFacade.getTrainee(username)));
    }

    @PutMapping("/{username}")
    @Operation(summary = "Update trainee profile")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile updated"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "404", description = "Trainee not found")
    })
    public ResponseEntity<TraineeProfileResponse> update(@PathVariable String username,
                                                         @Valid @RequestBody UpdateTraineeRequest request) {
        log.info("Updating trainee profile: {}", username);
        Trainee updated = gymFacade.updateTrainee(username, mapper.toTraineeEntity(request));
        return ResponseEntity.ok(mapper.toTraineeProfile(updated));
    }

    @DeleteMapping("/{username}")
    @Operation(summary = "Delete trainee profile", description = "Hard delete; cascades related trainings.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trainee deleted"),
            @ApiResponse(responseCode = "404", description = "Trainee not found")
    })
    public ResponseEntity<Void> delete(@PathVariable String username) {
        log.info("Deleting trainee profile: {}", username);
        gymFacade.deleteTrainee(username);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{username}/status")
    @Operation(summary = "Activate/De-activate trainee", description = "Non-idempotent: rejects a no-op change.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status changed"),
            @ApiResponse(responseCode = "400", description = "Already in the requested state"),
            @ApiResponse(responseCode = "404", description = "Trainee not found")
    })
    public ResponseEntity<Void> setStatus(@PathVariable String username,
                                          @Valid @RequestBody StatusRequest request) {
        log.info("Setting trainee '{}' active={}", username, request.isActive());
        gymFacade.setTraineeActive(username, request.isActive());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{username}/unassigned-trainers")
    @Operation(summary = "Get active trainers not assigned to the trainee")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trainers found"),
            @ApiResponse(responseCode = "404", description = "Trainee not found")
    })
    public ResponseEntity<List<TrainerBrief>> getUnassignedTrainers(@PathVariable String username) {
        log.info("Fetching unassigned trainers for trainee: {}", username);
        return ResponseEntity.ok(mapper.toTrainerBriefs(gymFacade.getUnassignedTrainers(username)));
    }

    @PutMapping("/{username}/trainers")
    @Operation(summary = "Update the trainee's trainers list")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trainers list updated"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "404", description = "Trainee or trainer not found")
    })
    public ResponseEntity<List<TrainerBrief>> updateTrainers(@PathVariable String username,
                                                             @Valid @RequestBody UpdateTraineeTrainersRequest request) {
        log.info("Updating trainers list for trainee: {}", username);
        Trainee updated = gymFacade.updateTraineeTrainers(username, request.trainerUsernames());
        return ResponseEntity.ok(mapper.toTraineeProfile(updated).trainers());
    }

    @GetMapping("/{username}/trainings")
    @Operation(summary = "Get trainee trainings list by optional criteria")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trainings found"),
            @ApiResponse(responseCode = "404", description = "Trainee not found")
    })
    public ResponseEntity<List<TrainingResponse>> getTrainings(
            @PathVariable String username,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodTo,
            @RequestParam(required = false) String trainerName,
            @RequestParam(required = false) String trainingType) {
        log.info("Fetching trainings for trainee: {}", username);
        return ResponseEntity.ok(mapper.toTrainingResponses(
                gymFacade.getTraineeTrainings(username, periodFrom, periodTo, trainerName, trainingType)));
    }
}
