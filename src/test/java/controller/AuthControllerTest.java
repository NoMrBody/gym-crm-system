package controller;

import exception.AuthenticationException;
import exception.GlobalExceptionHandler;
import facade.GymFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

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
    void login_validCredentials_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/login")
                        .param("username", "Jane.Smith")
                        .param("password", "secret"))
                .andExpect(status().isOk());
        verify(gymFacade).login("Jane.Smith", "secret");
    }

    @Test
    void login_invalidCredentials_returns401() throws Exception {
        doThrow(new AuthenticationException("Invalid username or password"))
                .when(gymFacade).login(anyString(), anyString());

        mockMvc.perform(get("/api/v1/login")
                        .param("username", "Jane.Smith")
                        .param("password", "wrong"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void changeLogin_blankNewPassword_returns400() throws Exception {
        String body = """
                {"username":"Jane.Smith","oldPassword":"old","newPassword":""}
                """;

        mockMvc.perform(put("/api/v1/login")
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
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
        verify(gymFacade).changeLogin("Jane.Smith", "old", "newSecret");
    }
}
