package controller;

import dto.TrainingTypeResponse;
import facade.GymFacade;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import mapper.DtoMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/training-types")
@Tag(name = "Training Type", description = "Reference data for training types")
public class TrainingTypeController {
    private static final Logger log = LoggerFactory.getLogger(TrainingTypeController.class);

    private final GymFacade gymFacade;
    private final DtoMapper mapper;

    public TrainingTypeController(GymFacade gymFacade, DtoMapper mapper) {
        this.gymFacade = gymFacade;
        this.mapper = mapper;
    }

    @GetMapping
    @Operation(summary = "Get training types", description = "Returns the constant list of training types.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Training types returned")
    })
    public ResponseEntity<List<TrainingTypeResponse>> getTrainingTypes() {
        log.info("Fetching training types");
        List<TrainingTypeResponse> types = gymFacade.getTrainingTypes().stream()
                .map(mapper::toTrainingType)
                .toList();
        return ResponseEntity.ok(types);
    }
}
