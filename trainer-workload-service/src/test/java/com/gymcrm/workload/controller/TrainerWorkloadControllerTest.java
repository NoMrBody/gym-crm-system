package com.gymcrm.workload.controller;

import com.gymcrm.workload.config.SecurityConfig;
import com.gymcrm.workload.config.TransactionLoggingFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contract and security checks against the real application context. The {@code jwt()}
 * post-processor stands in for gym-crm-core's signed token, so these tests cover the
 * authorisation rules without minting real JWTs.
 *
 * <p>All tests share one in-memory database, so each one uses its own trainer username.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TrainerWorkloadControllerTest {

    private static final String WORKLOADS = SecurityConfig.WORKLOADS_PATH;

    @Autowired
    private MockMvc mockMvc;

    /** A token like the one gym-crm-core mints for itself. */
    private static RequestPostProcessor serviceToken() {
        return jwt().jwt(jwt -> jwt.subject("gym-crm-core").claim("roles", List.of("ROLE_SERVICE")))
                .authorities(new SimpleGrantedAuthority("ROLE_SERVICE"));
    }

    /** A token like the one an end user gets after logging in to gym-crm-core. */
    private static RequestPostProcessor userToken() {
        return jwt().jwt(jwt -> jwt.subject("Jane.Smith").claim("roles", List.of("ROLE_TRAINEE")))
                .authorities(new SimpleGrantedAuthority("ROLE_TRAINEE"));
    }

    private static MockHttpServletRequestBuilder submit(String body) {
        return post(WORKLOADS).contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private static String event(String username, String date, int duration, String actionType) {
        return """
                {"trainerUsername":"%s","trainerFirstName":"Alice","trainerLastName":"Cooper",
                 "isActive":true,"trainingDate":"%s","trainingDuration":%d,"actionType":"%s"}
                """.formatted(username, date, duration, actionType);
    }

    private void report(String username, String date, int duration, String actionType) throws Exception {
        mockMvc.perform(submit(event(username, date, duration, actionType)).with(serviceToken()))
                .andExpect(status().isOk());
    }

    @Test
    void submitWorkload_withoutToken_returns401() throws Exception {
        mockMvc.perform(submit(event("No.Token", "2026-03-12", 60, "ADD")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication required"));
    }

    @Test
    void submitWorkload_withAUserToken_returns403() throws Exception {
        mockMvc.perform(submit(event("User.Token", "2026-03-12", 60, "ADD")).with(userToken()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied"));
    }

    @Test
    void submitWorkload_withTheServiceToken_returns200AndAnEmptyBody() throws Exception {
        mockMvc.perform(submit(event("Service.Token", "2026-03-12", 60, "ADD")).with(serviceToken()))
                .andExpect(status().isOk())
                .andExpect(content().string(""));
    }

    @Test
    void submitWorkload_withAnInvalidBody_returns400WithFieldDetails() throws Exception {
        String missingFields = """
                {"trainerUsername":"","trainingDuration":-5,"actionType":"ADD"}
                """;

        mockMvc.perform(submit(missingFields).with(serviceToken()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.details").isNotEmpty());
    }

    @Test
    void submitWorkload_withAnUnknownActionType_returns400() throws Exception {
        mockMvc.perform(submit(event("Bad.Action", "2026-03-12", 60, "ARCHIVE")).with(serviceToken()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed request body"));
    }

    @Test
    void getSummary_returnsTheTotalsGroupedByYearAndMonth() throws Exception {
        report("Summary.Trainer", "2026-03-12", 60, "ADD");
        report("Summary.Trainer", "2026-03-28", 45, "ADD");
        report("Summary.Trainer", "2027-01-05", 30, "ADD");

        mockMvc.perform(get(WORKLOADS + "/Summary.Trainer").with(userToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trainerUsername").value("Summary.Trainer"))
                .andExpect(jsonPath("$.trainerFirstName").value("Alice"))
                .andExpect(jsonPath("$.trainerLastName").value("Cooper"))
                .andExpect(jsonPath("$.trainerStatus").value("ACTIVE"))
                .andExpect(jsonPath("$.years.length()").value(2))
                .andExpect(jsonPath("$.years[0].year").value(2026))
                .andExpect(jsonPath("$.years[0].months[0].month").value(3))
                .andExpect(jsonPath("$.years[0].months[0].trainingSummaryDuration").value(105))
                .andExpect(jsonPath("$.years[1].year").value(2027))
                .andExpect(jsonPath("$.years[1].months[0].trainingSummaryDuration").value(30));
    }

    @Test
    void getSummary_withoutToken_returns401() throws Exception {
        mockMvc.perform(get(WORKLOADS + "/Anyone"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getSummary_unknownTrainer_returns404() throws Exception {
        mockMvc.perform(get(WORKLOADS + "/Ghost.Trainer").with(userToken()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("No workload recorded for trainer: Ghost.Trainer"));
    }

    @Test
    void getMonthlyWorkload_reflectsAddsAndDeletes() throws Exception {
        report("Monthly.Trainer", "2026-05-02", 90, "ADD");
        report("Monthly.Trainer", "2026-05-19", 60, "ADD");
        report("Monthly.Trainer", "2026-05-19", 60, "DELETE");

        mockMvc.perform(get(WORKLOADS + "/Monthly.Trainer/years/2026/months/5").with(userToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trainerUsername").value("Monthly.Trainer"))
                .andExpect(jsonPath("$.year").value(2026))
                .andExpect(jsonPath("$.month").value(5))
                .andExpect(jsonPath("$.trainingSummaryDuration").value(90));
    }

    @Test
    void getMonthlyWorkload_forAMonthWithoutTrainings_returnsZero() throws Exception {
        report("Empty.Month", "2026-05-02", 90, "ADD");

        mockMvc.perform(get(WORKLOADS + "/Empty.Month/years/2026/months/11").with(userToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trainingSummaryDuration").value(0));
    }

    @Test
    void getMonthlyWorkload_withAnOutOfRangeMonth_returns400() throws Exception {
        report("Bad.Month", "2026-05-02", 90, "ADD");

        mockMvc.perform(get(WORKLOADS + "/Bad.Month/years/2026/months/13").with(userToken()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    @Test
    void theTransactionIdOfTheCallerIsReusedAndEchoedBack() throws Exception {
        mockMvc.perform(get(WORKLOADS + "/Ghost.Trainer")
                        .header(TransactionLoggingFilter.TRANSACTION_HEADER, "core-transaction-1")
                        .with(userToken()))
                .andExpect(header().string(TransactionLoggingFilter.TRANSACTION_HEADER, "core-transaction-1"))
                .andExpect(jsonPath("$.transactionId").value("core-transaction-1"));
    }

    @Test
    void aTransactionIdIsGeneratedWhenTheCallerDoesNotSendOne() throws Exception {
        mockMvc.perform(get(WORKLOADS + "/Ghost.Trainer").with(userToken()))
                .andExpect(header().exists(TransactionLoggingFilter.TRANSACTION_HEADER))
                .andExpect(jsonPath("$.transactionId").isNotEmpty());
    }

    @Test
    void actuatorHealthStaysPublic() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }
}
