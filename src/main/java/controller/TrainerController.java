package controller;

import dto.CredentialsResponse;
import dto.StatusRequest;
import dto.TrainerProfileResponse;
import dto.TrainerRegistrationRequest;
import dto.TrainingResponse;
import dto.UpdateTrainerRequest;
import facade.GymFacade;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import mapper.DtoMapper;
import model.Trainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/api/v1/trainers")
@Tag(name = "Trainer", description = "Trainer registration and profile management")
public class TrainerController {
    private static final Logger log = LoggerFactory.getLogger(TrainerController.class);

    private final GymFacade gymFacade;
    private final DtoMapper mapper;

    public TrainerController(GymFacade gymFacade, DtoMapper mapper) {
        this.gymFacade = gymFacade;
        this.mapper = mapper;
    }

    @PostMapping
    @Operation(summary = "Register a trainer", description = "Creates a trainer profile and returns generated credentials.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Trainer created"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "404", description = "Training type not found")
    })
    public ResponseEntity<CredentialsResponse> register(@Valid @RequestBody TrainerRegistrationRequest request) {
        log.info("Registering trainer: {} {}", request.firstName(), request.lastName());
        Trainer saved = gymFacade.createTrainer(mapper.toTrainerEntity(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toCredentials(saved.getUser()));
    }

    @GetMapping("/{username}")
    @Operation(summary = "Get trainer profile")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile found"),
            @ApiResponse(responseCode = "404", description = "Trainer not found")
    })
    public ResponseEntity<TrainerProfileResponse> getProfile(@PathVariable String username) {
        log.info("Fetching trainer profile: {}", username);
        return ResponseEntity.ok(mapper.toTrainerProfile(gymFacade.getTrainer(username)));
    }

    @PutMapping("/{username}")
    @Operation(summary = "Update trainer profile", description = "Specialization is read-only.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile updated"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "404", description = "Trainer not found")
    })
    public ResponseEntity<TrainerProfileResponse> update(@PathVariable String username,
                                                         @Valid @RequestBody UpdateTrainerRequest request) {
        log.info("Updating trainer profile: {}", username);
        Trainer updated = gymFacade.updateTrainer(username, mapper.toTrainerEntity(request));
        return ResponseEntity.ok(mapper.toTrainerProfile(updated));
    }

    @PatchMapping("/{username}/status")
    @Operation(summary = "Activate/De-activate trainer", description = "Non-idempotent: rejects a no-op change.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status changed"),
            @ApiResponse(responseCode = "400", description = "Already in the requested state"),
            @ApiResponse(responseCode = "404", description = "Trainer not found")
    })
    public ResponseEntity<Void> setStatus(@PathVariable String username,
                                          @Valid @RequestBody StatusRequest request) {
        log.info("Setting trainer '{}' active={}", username, request.isActive());
        gymFacade.setTrainerActive(username, request.isActive());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{username}/trainings")
    @Operation(summary = "Get trainer trainings list by optional criteria")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trainings found"),
            @ApiResponse(responseCode = "404", description = "Trainer not found")
    })
    public ResponseEntity<List<TrainingResponse>> getTrainings(
            @PathVariable String username,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodTo,
            @RequestParam(required = false) String traineeName) {
        log.info("Fetching trainings for trainer: {}", username);
        return ResponseEntity.ok(mapper.toTrainingResponses(
                gymFacade.getTrainerTrainings(username, periodFrom, periodTo, traineeName)));
    }
}
