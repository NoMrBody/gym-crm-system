package com.gymcrm.workload.service;

import com.gymcrm.workload.dto.MonthlyWorkloadResponse;
import com.gymcrm.workload.dto.TrainerWorkloadRequest;
import com.gymcrm.workload.dto.TrainerWorkloadSummaryResponse;
import com.gymcrm.workload.dto.TrainerWorkloadSummaryResponse.MonthSummary;
import com.gymcrm.workload.dto.TrainerWorkloadSummaryResponse.TrainerStatus;
import com.gymcrm.workload.dto.TrainerWorkloadSummaryResponse.YearSummary;
import com.gymcrm.workload.model.ActionType;
import com.gymcrm.workload.model.TrainerWorkload;
import com.gymcrm.workload.repository.TrainerWorkloadRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TrainerWorkloadServiceTest {

    private static final String USERNAME = "Alice.Cooper";

    private TrainerWorkloadService service;

    @BeforeEach
    void setUp() {
        service = new TrainerWorkloadService(inMemoryRepository());
    }

    @Test
    void add_createsTheTrainerAndTheMonthBucket() {
        service.apply(request(ActionType.ADD, LocalDate.of(2026, 3, 12), 60));

        TrainerWorkloadSummaryResponse summary = service.getSummary(USERNAME);
        assertEquals(USERNAME, summary.trainerUsername());
        assertEquals("Alice", summary.trainerFirstName());
        assertEquals("Cooper", summary.trainerLastName());
        assertEquals(TrainerStatus.ACTIVE, summary.trainerStatus());
        assertEquals(List.of(new YearSummary(2026, List.of(new MonthSummary(3, 60)))), summary.years());
    }

    @Test
    void add_accumulatesWithinTheSameMonthAndKeepsMonthsApart() {
        service.apply(request(ActionType.ADD, LocalDate.of(2026, 3, 12), 60));
        service.apply(request(ActionType.ADD, LocalDate.of(2026, 3, 28), 45));
        service.apply(request(ActionType.ADD, LocalDate.of(2026, 4, 1), 30));

        assertEquals(105, service.getMonthlyWorkload(USERNAME, 2026, 3).trainingSummaryDuration());
        assertEquals(30, service.getMonthlyWorkload(USERNAME, 2026, 4).trainingSummaryDuration());
    }

    @Test
    void delete_subtractsFromTheMonthTotal() {
        service.apply(request(ActionType.ADD, LocalDate.of(2026, 3, 12), 60));
        service.apply(request(ActionType.ADD, LocalDate.of(2026, 3, 28), 45));

        service.apply(request(ActionType.DELETE, LocalDate.of(2026, 3, 28), 45));

        assertEquals(60, service.getMonthlyWorkload(USERNAME, 2026, 3).trainingSummaryDuration());
    }

    @Test
    void delete_removesTheBucketOnceItReachesZero() {
        service.apply(request(ActionType.ADD, LocalDate.of(2026, 3, 12), 60));

        service.apply(request(ActionType.DELETE, LocalDate.of(2026, 3, 12), 60));

        assertTrue(service.getSummary(USERNAME).years().isEmpty());
        assertEquals(0, service.getMonthlyWorkload(USERNAME, 2026, 3).trainingSummaryDuration());
    }

    @Test
    void delete_clampsAtZeroWhenCancellingMoreThanRecorded() {
        service.apply(request(ActionType.ADD, LocalDate.of(2026, 3, 12), 30));

        service.apply(request(ActionType.DELETE, LocalDate.of(2026, 3, 12), 90));

        assertEquals(0, service.getMonthlyWorkload(USERNAME, 2026, 3).trainingSummaryDuration());
    }

    @Test
    void delete_forAnUnknownMonthLeavesTheOtherMonthsUntouched() {
        service.apply(request(ActionType.ADD, LocalDate.of(2026, 3, 12), 30));

        service.apply(request(ActionType.DELETE, LocalDate.of(2026, 7, 1), 60));

        assertEquals(30, service.getMonthlyWorkload(USERNAME, 2026, 3).trainingSummaryDuration());
        assertEquals(0, service.getMonthlyWorkload(USERNAME, 2026, 7).trainingSummaryDuration());
    }

    @Test
    void getSummary_ordersYearsAndMonthsAscending() {
        service.apply(request(ActionType.ADD, LocalDate.of(2027, 1, 5), 10));
        service.apply(request(ActionType.ADD, LocalDate.of(2025, 12, 5), 20));
        service.apply(request(ActionType.ADD, LocalDate.of(2025, 2, 5), 30));

        List<YearSummary> years = service.getSummary(USERNAME).years();

        assertEquals(List.of(
                new YearSummary(2025, List.of(new MonthSummary(2, 30), new MonthSummary(12, 20))),
                new YearSummary(2027, List.of(new MonthSummary(1, 10)))), years);
    }

    @Test
    void apply_refreshesTheTrainerDetailsFromTheLatestEvent() {
        service.apply(request(ActionType.ADD, LocalDate.of(2026, 3, 12), 60));

        service.apply(new TrainerWorkloadRequest(USERNAME, "Alicia", "Cooper-Smith", false,
                LocalDate.of(2026, 3, 20), 30, ActionType.ADD));

        TrainerWorkloadSummaryResponse summary = service.getSummary(USERNAME);
        assertEquals("Alicia", summary.trainerFirstName());
        assertEquals("Cooper-Smith", summary.trainerLastName());
        assertEquals(TrainerStatus.INACTIVE, summary.trainerStatus());
    }

    @Test
    void getSummary_unknownTrainer_throwsEntityNotFound() {
        assertThrows(EntityNotFoundException.class, () -> service.getSummary("Ghost.Trainer"));
    }

    @Test
    void getMonthlyWorkload_unknownTrainer_throwsEntityNotFound() {
        assertThrows(EntityNotFoundException.class,
                () -> service.getMonthlyWorkload("Ghost.Trainer", 2026, 3));
    }

    @Test
    void getMonthlyWorkload_echoesBackTheRequestedPeriod() {
        service.apply(request(ActionType.ADD, LocalDate.of(2026, 3, 12), 60));

        MonthlyWorkloadResponse response = service.getMonthlyWorkload(USERNAME, 2026, 3);

        assertEquals(USERNAME, response.trainerUsername());
        assertEquals(2026, response.year());
        assertEquals(3, response.month());
    }

    private static TrainerWorkloadRequest request(ActionType actionType, LocalDate date, int duration) {
        return new TrainerWorkloadRequest(USERNAME, "Alice", "Cooper", true, date, duration, actionType);
    }

    /**
     * The service only reads and writes single aggregates, so a map behind findById/save
     * is enough to exercise the accumulation logic without a Spring context.
     */
    private static TrainerWorkloadRepository inMemoryRepository() {
        Map<String, TrainerWorkload> store = new HashMap<>();
        TrainerWorkloadRepository repository = mock(TrainerWorkloadRepository.class);

        when(repository.findById(anyString()))
                .thenAnswer(invocation -> Optional.ofNullable(store.get(invocation.<String>getArgument(0))));
        when(repository.save(any(TrainerWorkload.class))).thenAnswer(invocation -> {
            TrainerWorkload entity = invocation.getArgument(0);
            store.put(entity.getTrainerUsername(), entity);
            return entity;
        });

        return repository;
    }
}
