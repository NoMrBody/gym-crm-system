package controller;

import dto.ChangeLoginRequest;
import facade.GymFacade;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Authentication", description = "Login and credential management")
public class AuthController {
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final GymFacade gymFacade;

    public AuthController(GymFacade gymFacade) {
        this.gymFacade = gymFacade;
    }

    @GetMapping("/login")
    @Operation(summary = "Login", description = "Verifies that the username and password match.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Credentials are valid"),
            @ApiResponse(responseCode = "401", description = "Invalid username or password")
    })
    public ResponseEntity<Void> login(
            @Parameter(description = "Username", required = true) @RequestParam String username,
            @Parameter(description = "Password", required = true) @RequestParam String password) {
        log.info("Login attempt for username: {}", username);
        gymFacade.login(username, password);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/login")
    @Operation(summary = "Change login password", description = "Changes the password after verifying the current one.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Password changed"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Invalid username or old password")
    })
    public ResponseEntity<Void> changeLogin(@Valid @RequestBody ChangeLoginRequest request) {
        log.info("Change login for username: {}", request.username());
        gymFacade.changeLogin(request.username(), request.oldPassword(), request.newPassword());
        return ResponseEntity.ok().build();
    }
}
