package controller;

import exception.GlobalExceptionHandler;
import facade.GymFacade;
import jakarta.persistence.EntityNotFoundException;
import mapper.DtoMapper;
import model.Trainee;
import model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TraineeControllerTest {

    @Mock
    private GymFacade gymFacade;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TraineeController(gymFacade, new DtoMapper()))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private static Trainee trainee(String username, String password, boolean active) {
        User user = new User();
        user.setFirstName("Jane");
        user.setLastName("Smith");
        user.setUsername(username);
        user.setPassword(password);
        user.setActive(active);
        Trainee trainee = new Trainee();
        trainee.setUser(user);
        return trainee;
    }

    @Test
    void register_valid_returns201WithCredentials() throws Exception {
        when(gymFacade.createTrainee(any())).thenReturn(trainee("Jane.Smith", "pw12345678", true));
        String body = """
                {"firstName":"Jane","lastName":"Smith"}
                """;

        mockMvc.perform(post("/api/v1/trainees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("Jane.Smith"))
                .andExpect(jsonPath("$.password").value("pw12345678"));
    }

    @Test
    void register_blankFirstName_returns400() throws Exception {
        String body = """
                {"firstName":"","lastName":"Smith"}
                """;

        mockMvc.perform(post("/api/v1/trainees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getProfile_found_returns200() throws Exception {
        when(gymFacade.getTrainee("Jane.Smith")).thenReturn(trainee("Jane.Smith", "pw", true));

        mockMvc.perform(get("/api/v1/trainees/Jane.Smith"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Jane"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void getProfile_notFound_returns404() throws Exception {
        doThrow(new EntityNotFoundException("Trainee not found with username: Ghost"))
                .when(gymFacade).getTrainee(anyString());

        mockMvc.perform(get("/api/v1/trainees/Ghost"))
                .andExpect(status().isNotFound());
    }
}
