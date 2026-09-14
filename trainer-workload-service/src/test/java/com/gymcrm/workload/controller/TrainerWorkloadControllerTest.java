package com.gymcrm.workload.controller;

import com.gymcrm.workload.AbstractMongoIntegrationTest;
import com.gymcrm.workload.config.SecurityConfig;
import com.gymcrm.workload.config.TransactionLoggingFilter;
import com.gymcrm.workload.dto.TrainerWorkloadRequest;
import com.gymcrm.workload.model.ActionType;
import com.gymcrm.workload.service.TrainerWorkloadService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDate;
import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contract and security checks against the real application context. The {@code jwt()}
 * post-processor stands in for an end-user token, so these tests cover the
 * authorisation rules without minting real JWTs.
 *
 * <p>All tests share one MongoDB container, so each one uses its own trainer username.
 * Workload documents are seeded through the service: updates now arrive over JMS, not REST.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TrainerWorkloadControllerTest extends AbstractMongoIntegrationTest {

    private static final String WORKLOADS = SecurityConfig.WORKLOADS_PATH;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TrainerWorkloadService workloadService;

    /** A token like the one an end user gets after logging in to gym-crm-core. */
    private static RequestPostProcessor userToken() {
        return jwt().jwt(jwt -> jwt.subject("Jane.Smith").claim("roles", List.of("ROLE_TRAINEE")))
                .authorities(new SimpleGrantedAuthority("ROLE_TRAINEE"));
    }

    private void report(String username, String date, int duration, ActionType actionType) {
        workloadService.apply(new TrainerWorkloadRequest(
                username, "Alice", "Cooper", true, LocalDate.parse(date), duration, actionType));
    }

    @Test
    void getSummary_returnsTheTotalsGroupedByYearAndMonth() throws Exception {
        report("Summary.Trainer", "2026-03-12", 60, ActionType.ADD);
        report("Summary.Trainer", "2026-03-28", 45, ActionType.ADD);
        report("Summary.Trainer", "2027-01-05", 30, ActionType.ADD);

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
        report("Monthly.Trainer", "2026-05-02", 90, ActionType.ADD);
        report("Monthly.Trainer", "2026-05-19", 60, ActionType.ADD);
        report("Monthly.Trainer", "2026-05-19", 60, ActionType.DELETE);

        mockMvc.perform(get(WORKLOADS + "/Monthly.Trainer/years/2026/months/5").with(userToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trainerUsername").value("Monthly.Trainer"))
                .andExpect(jsonPath("$.year").value(2026))
                .andExpect(jsonPath("$.month").value(5))
                .andExpect(jsonPath("$.trainingSummaryDuration").value(90));
    }

    @Test
    void getMonthlyWorkload_forAMonthWithoutTrainings_returnsZero() throws Exception {
        report("Empty.Month", "2026-05-02", 90, ActionType.ADD);

        mockMvc.perform(get(WORKLOADS + "/Empty.Month/years/2026/months/11").with(userToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trainingSummaryDuration").value(0));
    }

    @Test
    void getMonthlyWorkload_withAnOutOfRangeMonth_returns400() throws Exception {
        report("Bad.Month", "2026-05-02", 90, ActionType.ADD);

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
