package controller;

import dto.ChangeLoginRequest;
import dto.LoginRequest;
import dto.TokenResponse;
import facade.GymFacade;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Authentication", description = "Login, logout and credential management")
public class AuthController {
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final GymFacade gymFacade;

    public AuthController(GymFacade gymFacade) {
        this.gymFacade = gymFacade;
    }

    @PostMapping("/login")
    @SecurityRequirements
    @Operation(summary = "Login", description = "Verifies the credentials and returns a bearer token.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token issued"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Invalid username or password"),
            @ApiResponse(responseCode = "423", description = "Blocked after too many failed attempts")
    })
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login attempt for username: {}", request.username());
        return ResponseEntity.ok(gymFacade.login(request.username(), request.password()));
    }

    @PutMapping("/login")
    @Operation(summary = "Change login password",
            description = "Changes the password of the authenticated user after verifying the current one.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Password changed"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Invalid username or old password"),
            @ApiResponse(responseCode = "403", description = "Attempt to change another user's password")
    })
    public ResponseEntity<Void> changeLogin(@Valid @RequestBody ChangeLoginRequest request,
                                            Authentication authentication) {
        if (!request.username().equals(authentication.getName())) {
            log.warn("User '{}' attempted to change the password of '{}'",
                    authentication.getName(), request.username());
            throw new AccessDeniedException("Cannot change the password of another user");
        }
        log.info("Change login for username: {}", request.username());
        gymFacade.changeLogin(request.username(), request.oldPassword(), request.newPassword());
        return ResponseEntity.ok().build();
    }
}
