package controller;

import dto.TokenResponse;
import exception.AuthenticationException;
import exception.GlobalExceptionHandler;
import exception.UserBlockedException;
import facade.GymFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private static final Authentication JANE = UsernamePasswordAuthenticationToken.authenticated(
            "Jane.Smith", null, List.of());

    @Mock
    private GymFacade gymFacade;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(gymFacade))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void login_validCredentials_returns200WithToken() throws Exception {
        when(gymFacade.login("Jane.Smith", "secret"))
                .thenReturn(new TokenResponse("token-value", "Bearer", 3600));
        String body = """
                {"username":"Jane.Smith","password":"secret"}
                """;

        mockMvc.perform(post("/api/v1/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("token-value"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600));
    }

    @Test
    void login_invalidCredentials_returns401() throws Exception {
        doThrow(new AuthenticationException("Invalid username or password"))
                .when(gymFacade).login(anyString(), anyString());
        String body = """
                {"username":"Jane.Smith","password":"wrong"}
                """;

        mockMvc.perform(post("/api/v1/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_blockedUser_returns423() throws Exception {
        doThrow(new UserBlockedException("Too many failed login attempts. Try again in 5 minute(s)"))
                .when(gymFacade).login(anyString(), anyString());
        String body = """
                {"username":"Jane.Smith","password":"secret"}
                """;

        mockMvc.perform(post("/api/v1/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isLocked());
    }

    @Test
    void login_blankUsername_returns400() throws Exception {
        String body = """
                {"username":"","password":"secret"}
                """;

        mockMvc.perform(post("/api/v1/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void changeLogin_blankNewPassword_returns400() throws Exception {
        String body = """
                {"username":"Jane.Smith","oldPassword":"old","newPassword":""}
                """;

        mockMvc.perform(put("/api/v1/login")
                        .principal(JANE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void changeLogin_valid_returns200() throws Exception {
        String body = """
                {"username":"Jane.Smith","oldPassword":"old","newPassword":"newSecret"}
                """;

        mockMvc.perform(put("/api/v1/login")
                        .principal(JANE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
        verify(gymFacade).changeLogin("Jane.Smith", "old", "newSecret");
    }

    @Test
    void changeLogin_forAnotherUser_returns403() throws Exception {
        String body = """
                {"username":"Alice.Cooper","oldPassword":"old","newPassword":"newSecret"}
                """;

        mockMvc.perform(put("/api/v1/login")
                        .principal(JANE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
        verify(gymFacade, never()).changeLogin(anyString(), anyString(), anyString());
    }
}
