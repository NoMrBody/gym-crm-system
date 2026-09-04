package com.gymcrm.workload.service;

import com.gymcrm.workload.dto.TrainerWorkloadRequest;
import com.gymcrm.workload.dto.MonthlyWorkloadResponse;
import com.gymcrm.workload.dto.TrainerWorkloadSummaryResponse;
import com.gymcrm.workload.dto.TrainerWorkloadSummaryResponse.MonthSummary;
import com.gymcrm.workload.dto.TrainerWorkloadSummaryResponse.TrainerStatus;
import com.gymcrm.workload.dto.TrainerWorkloadSummaryResponse.YearSummary;
import com.gymcrm.workload.model.MonthlyWorkload;
import com.gymcrm.workload.model.TrainerWorkload;
import com.gymcrm.workload.repository.TrainerWorkloadRepository;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Maintains the monthly training totals per trainer. The trainer's own details are
 * refreshed from every incoming event, so the summary always reflects the latest profile.
 */
@Service
public class TrainerWorkloadService {
    private static final Logger log = LoggerFactory.getLogger(TrainerWorkloadService.class);

    private final TrainerWorkloadRepository repository;

    public TrainerWorkloadService(TrainerWorkloadRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void apply(TrainerWorkloadRequest request) {
        int year = request.trainingDate().getYear();
        int month = request.trainingDate().getMonthValue();

        TrainerWorkload workload = repository.findById(request.trainerUsername())
                .orElseGet(() -> newWorkload(request.trainerUsername()));
        workload.setFirstName(request.trainerFirstName());
        workload.setLastName(request.trainerLastName());
        workload.setActive(request.isActive());

        switch (request.actionType()) {
            case ADD -> add(workload, year, month, request.trainingDuration());
            case DELETE -> subtract(workload, year, month, request.trainingDuration());
        }

        repository.save(workload);
        log.info("Applied {} of {} minutes for trainer '{}' in {}-{}: month total is now {} minutes",
                request.actionType(), request.trainingDuration(), request.trainerUsername(),
                year, month, durationOf(workload, year, month));
    }

    @Transactional(readOnly = true)
    public TrainerWorkloadSummaryResponse getSummary(String trainerUsername) {
        TrainerWorkload workload = requireTrainer(trainerUsername);
        log.debug("Building workload summary for trainer '{}'", trainerUsername);

        // TreeMaps keep both years and months in ascending order without a final sort pass.
        Map<Integer, Map<Integer, Integer>> byYear = new TreeMap<>();
        for (MonthlyWorkload monthly : workload.getMonthlyWorkloads()) {
            byYear.computeIfAbsent(monthly.getTrainingYear(), year -> new TreeMap<>())
                    .merge(monthly.getTrainingMonth(), monthly.getTotalDuration(), Integer::sum);
        }

        List<YearSummary> years = byYear.entrySet().stream()
                .map(yearEntry -> new YearSummary(yearEntry.getKey(),
                        yearEntry.getValue().entrySet().stream()
                                .map(monthEntry -> new MonthSummary(monthEntry.getKey(), monthEntry.getValue()))
                                .toList()))
                .toList();

        return new TrainerWorkloadSummaryResponse(
                workload.getTrainerUsername(),
                workload.getFirstName(),
                workload.getLastName(),
                TrainerStatus.of(workload.isActive()),
                years);
    }

    @Transactional(readOnly = true)
    public MonthlyWorkloadResponse getMonthlyWorkload(String trainerUsername, int year, int month) {
        TrainerWorkload workload = requireTrainer(trainerUsername);
        log.debug("Fetching {}-{} workload for trainer '{}'", year, month, trainerUsername);
        return new MonthlyWorkloadResponse(trainerUsername, year, month, durationOf(workload, year, month));
    }

    private void add(TrainerWorkload workload, int year, int month, int duration) {
        find(workload, year, month).ifPresentOrElse(
                monthly -> monthly.setTotalDuration(monthly.getTotalDuration() + duration),
                () -> workload.getMonthlyWorkloads().add(new MonthlyWorkload(year, month, duration)));
    }

    private void subtract(TrainerWorkload workload, int year, int month, int duration) {
        find(workload, year, month).ifPresentOrElse(monthly -> {
            int remaining = monthly.getTotalDuration() - duration;
            if (remaining < 0) {
                log.warn("Cancelling {} minutes for trainer '{}' in {}-{} exceeds the recorded {} minutes; clamping to 0",
                        duration, workload.getTrainerUsername(), year, month, monthly.getTotalDuration());
                remaining = 0;
            }
            if (remaining == 0) {
                workload.getMonthlyWorkloads().remove(monthly);
            } else {
                monthly.setTotalDuration(remaining);
            }
        }, () -> log.warn("Cancelling {} minutes for trainer '{}' in {}-{}, but no workload is recorded for that month",
                duration, workload.getTrainerUsername(), year, month));
    }

    private static Optional<MonthlyWorkload> find(TrainerWorkload workload, int year, int month) {
        return workload.getMonthlyWorkloads().stream()
                .filter(monthly -> monthly.isFor(year, month))
                .findFirst();
    }

    private static int durationOf(TrainerWorkload workload, int year, int month) {
        return find(workload, year, month)
                .map(MonthlyWorkload::getTotalDuration)
                .orElse(0);
    }

    private TrainerWorkload requireTrainer(String trainerUsername) {
        return repository.findById(trainerUsername)
                .orElseThrow(() -> new EntityNotFoundException(
                        "No workload recorded for trainer: " + trainerUsername));
    }

    private static TrainerWorkload newWorkload(String trainerUsername) {
        TrainerWorkload workload = new TrainerWorkload();
        workload.setTrainerUsername(trainerUsername);
        workload.setMonthlyWorkloads(new ArrayList<>());
        return workload;
    }
}
