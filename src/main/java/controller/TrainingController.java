package controller;

import dto.AddTrainingRequest;
import facade.GymFacade;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/trainings")
@Tag(name = "Training", description = "Training management")
public class TrainingController {
    private static final Logger log = LoggerFactory.getLogger(TrainingController.class);

    private final GymFacade gymFacade;

    public TrainingController(GymFacade gymFacade) {
        this.gymFacade = gymFacade;
    }

    @PostMapping
    @Operation(summary = "Add a training",
            description = "Creates a training; the training type is derived from the trainer's specialization.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Training added"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "404", description = "Trainee or trainer not found")
    })
    public ResponseEntity<Void> addTraining(@Valid @RequestBody AddTrainingRequest request) {
        log.info("Adding training '{}' trainee={} trainer={}",
                request.trainingName(), request.traineeUsername(), request.trainerUsername());
        gymFacade.addTraining(request.traineeUsername(), request.trainerUsername(),
                request.trainingName(), request.trainingDate(), request.trainingDuration());
        return ResponseEntity.ok().build();
    }
}
