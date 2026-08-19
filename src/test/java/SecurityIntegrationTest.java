import com.jayway.jsonpath.JsonPath;
import model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import repository.UserRepository;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end checks of the security chain against the real application context.
 *
 * <p>Lives in the default package so it can name {@code Application}, which the package scan
 * cannot reach. Each test registers its own user so the in-memory brute-force and denylist
 * state cannot leak between tests.
 */
@SpringBootTest(classes = Application.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    /** Keeps the scheduled stats query from racing the tests for the H2 connection. */
    @MockitoBean
    private metrics.GymStatsCollector gymStatsCollector;

    private record Credentials(String username, String password, String token) {
    }

    private Credentials registerTrainee(String firstName, String lastName) throws Exception {
        String body = """
                {"firstName":"%s","lastName":"%s"}
                """.formatted(firstName, lastName);

        String response = mockMvc.perform(post("/api/v1/trainees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return new Credentials(
                JsonPath.read(response, "$.username"),
                JsonPath.read(response, "$.password"),
                JsonPath.read(response, "$.accessToken"));
    }

    private String login(String username, String password) throws Exception {
        String body = """
                {"username":"%s","password":"%s"}
                """.formatted(username, password);

        String response = mockMvc.perform(post("/api/v1/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return JsonPath.read(response, "$.accessToken");
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    @Test
    void registerTrainee_isPublicAndReturnsCredentialsWithToken() throws Exception {
        mockMvc.perform(post("/api/v1/trainees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Public","lastName":"Trainee"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("Public.Trainee"))
                .andExpect(jsonPath("$.password").isNotEmpty())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    void registerTrainer_isPublicAndReturnsCredentialsWithToken() throws Exception {
        mockMvc.perform(post("/api/v1/trainers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Public","lastName":"Trainer","specializationId":1}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("Public.Trainer"))
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Test
    void passwordIsStoredAsASaltedHash() throws Exception {
        Credentials credentials = registerTrainee("Hashed", "Trainee");

        User stored = userRepository.findByUsername(credentials.username()).orElseThrow();

        assertFalse(stored.getPassword().equals(credentials.password()),
                "The raw password must never be persisted");
        assertTrue(stored.getPassword().startsWith("{bcrypt}$2a$"));
        assertTrue(passwordEncoder.matches(credentials.password(), stored.getPassword()));
    }

    @Test
    void twoUsersWithTheSamePasswordGetDifferentHashes() throws Exception {
        // BCrypt salts every hash, so the same input never produces the same output.
        String hashA = passwordEncoder.encode("identical-password");
        String hashB = passwordEncoder.encode("identical-password");

        assertFalse(hashA.equals(hashB));
        assertTrue(passwordEncoder.matches("identical-password", hashA));
        assertTrue(passwordEncoder.matches("identical-password", hashB));
    }

    @Test
    void protectedEndpointWithoutToken_returns401() throws Exception {
        Credentials credentials = registerTrainee("Unauthenticated", "Trainee");

        mockMvc.perform(get("/api/v1/trainees/" + credentials.username()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication required"));
    }

    @Test
    void protectedEndpointWithGarbageToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/training-types")
                        .header(HttpHeaders.AUTHORIZATION, bearer("not-a-jwt")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpointWithRegistrationToken_returns200() throws Exception {
        Credentials credentials = registerTrainee("Registered", "Trainee");

        mockMvc.perform(get("/api/v1/trainees/" + credentials.username())
                        .header(HttpHeaders.AUTHORIZATION, bearer(credentials.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(credentials.username()));
    }

    @Test
    void loginWithGeneratedPassword_returnsUsableToken() throws Exception {
        Credentials credentials = registerTrainee("Login", "Trainee");

        String token = login(credentials.username(), credentials.password());

        mockMvc.perform(get("/api/v1/training-types")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk());
    }

    @Test
    void loginWithWrongPassword_returns401() throws Exception {
        Credentials credentials = registerTrainee("Wrong", "Password");

        mockMvc.perform(post("/api/v1/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"definitely-wrong"}
                                """.formatted(credentials.username())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void threeFailedLogins_blockTheUserWith423() throws Exception {
        Credentials credentials = registerTrainee("Brute", "Force");
        String wrongCredentials = """
                {"username":"%s","password":"definitely-wrong"}
                """.formatted(credentials.username());

        for (int attempt = 1; attempt <= 3; attempt++) {
            mockMvc.perform(post("/api/v1/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(wrongCredentials))
                    .andExpect(status().isUnauthorized());
        }

        // The block applies even to the correct password.
        mockMvc.perform(post("/api/v1/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s"}
                                """.formatted(credentials.username(), credentials.password())))
                .andExpect(status().isLocked())
                .andExpect(jsonPath("$.message").value("Too many failed login attempts. Try again in 5 minute(s)"));
    }

    @Test
    void successfulLoginResetsTheFailureCounter() throws Exception {
        Credentials credentials = registerTrainee("Counter", "Reset");
        String wrongCredentials = """
                {"username":"%s","password":"definitely-wrong"}
                """.formatted(credentials.username());

        for (int attempt = 1; attempt <= 2; attempt++) {
            mockMvc.perform(post("/api/v1/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(wrongCredentials))
                    .andExpect(status().isUnauthorized());
        }
        login(credentials.username(), credentials.password());

        // Two more failures would have tripped the block had the counter not been cleared.
        for (int attempt = 1; attempt <= 2; attempt++) {
            mockMvc.perform(post("/api/v1/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(wrongCredentials))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Test
    void logoutRevokesTheToken() throws Exception {
        Credentials credentials = registerTrainee("Logout", "Trainee");
        String token = login(credentials.username(), credentials.password());

        mockMvc.perform(get("/api/v1/training-types")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/logout")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/training-types")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutWithoutToken_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/logout"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutLeavesOtherTokensUsable() throws Exception {
        Credentials credentials = registerTrainee("Two", "Sessions");
        String firstToken = login(credentials.username(), credentials.password());
        String secondToken = login(credentials.username(), credentials.password());

        mockMvc.perform(post("/api/v1/logout")
                        .header(HttpHeaders.AUTHORIZATION, bearer(firstToken)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/training-types")
                        .header(HttpHeaders.AUTHORIZATION, bearer(secondToken)))
                .andExpect(status().isOk());
    }

    @Test
    void changePassword_requiresAuthentication() throws Exception {
        Credentials credentials = registerTrainee("Change", "Unauthenticated");

        mockMvc.perform(put("/api/v1/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","oldPassword":"%s","newPassword":"BrandNewPass1"}
                                """.formatted(credentials.username(), credentials.password())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void changePassword_storesTheNewHashAndInvalidatesTheOldPassword() throws Exception {
        Credentials credentials = registerTrainee("Change", "Password");
        String token = login(credentials.username(), credentials.password());

        mockMvc.perform(put("/api/v1/login")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","oldPassword":"%s","newPassword":"BrandNewPass1"}
                                """.formatted(credentials.username(), credentials.password())))
                .andExpect(status().isOk());

        User stored = userRepository.findByUsername(credentials.username()).orElseThrow();
        assertTrue(passwordEncoder.matches("BrandNewPass1", stored.getPassword()));

        login(credentials.username(), "BrandNewPass1");
    }

    @Test
    void changePassword_forAnotherUser_returns403() throws Exception {
        Credentials attacker = registerTrainee("Nosy", "Trainee");
        Credentials victim = registerTrainee("Target", "Trainee");
        String token = login(attacker.username(), attacker.password());

        mockMvc.perform(put("/api/v1/login")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","oldPassword":"%s","newPassword":"BrandNewPass1"}
                                """.formatted(victim.username(), victim.password())))
                .andExpect(status().isForbidden());
    }

    @Test
    void corsPreflightIsAllowedFromAConfiguredOriginWithoutAToken() throws Exception {
        mockMvc.perform(options("/api/v1/training-types")
                        .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3000"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
    }

    @Test
    void corsPreflightFromAnUnknownOriginIsRejected() throws Exception {
        mockMvc.perform(options("/api/v1/training-types")
                        .header(HttpHeaders.ORIGIN, "http://evil.example.com")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isForbidden());
    }

    @Test
    void openApiDocsRemainPublic() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk());
    }
}
