package com.gymcrm.workload.service;

import com.gymcrm.workload.dto.MonthlyWorkloadResponse;
import com.gymcrm.workload.dto.TrainerWorkloadRequest;
import com.gymcrm.workload.dto.TrainerWorkloadSummaryResponse;
import com.gymcrm.workload.dto.TrainerWorkloadSummaryResponse.MonthSummary;
import com.gymcrm.workload.dto.TrainerWorkloadSummaryResponse.TrainerStatus;
import com.gymcrm.workload.dto.TrainerWorkloadSummaryResponse.YearSummary;
import com.gymcrm.workload.exception.TrainerNotFoundException;
import com.gymcrm.workload.model.MonthlyWorkload;
import com.gymcrm.workload.model.TrainerWorkloadDocument;
import com.gymcrm.workload.model.YearWorkload;
import com.gymcrm.workload.repository.TrainerWorkloadRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Maintains the monthly training totals per trainer. The trainer's own details are
 * refreshed from every incoming event, so the summary always reflects the latest profile.
 *
 * <p>One trainer is one MongoDB document, so each event is a single-document write and
 * needs no transaction.
 */
@Service
public class TrainerWorkloadService {
    private static final Logger log = LoggerFactory.getLogger(TrainerWorkloadService.class);

    private final TrainerWorkloadRepository repository;

    public TrainerWorkloadService(TrainerWorkloadRepository repository) {
        this.repository = repository;
    }

    public void apply(TrainerWorkloadRequest request) {
        String username = request.trainerUsername();
        int year = request.trainingDate().getYear();
        int month = request.trainingDate().getMonthValue();

        Optional<TrainerWorkloadDocument> existing = repository.findByUsername(username);
        TrainerWorkloadDocument document = existing.orElseGet(() -> newDocument(username));

        document.setTrainerFirstName(request.trainerFirstName());
        document.setTrainerLastName(request.trainerLastName());
        document.setTrainerStatus(request.isActive());

        switch (request.actionType()) {
            case ADD -> add(document, year, month, request.trainingDuration());
            case DELETE -> subtract(document, year, month, request.trainingDuration());
        }

        log.debug("Mongo save on trainer_workloads '{}': {} year bucket(s)",
                username, document.getYears().size());
        repository.save(document);

        log.info("{} {} of {} minutes for trainer '{}' in {}-{}: month total is now {} minutes",
                existing.isPresent() ? "Applied" : "Created a document and applied",
                request.actionType(), request.trainingDuration(), username, year, month,
                durationOf(document, year, month));
    }

    public TrainerWorkloadSummaryResponse getSummary(String trainerUsername) {
        TrainerWorkloadDocument document = requireTrainer(trainerUsername);
        log.debug("Building workload summary for trainer '{}'", trainerUsername);

        // The document already groups by year, so the summary only needs an ascending order.
        List<YearSummary> years = document.getYears().stream()
                .sorted(Comparator.comparingInt(YearWorkload::getYear))
                .map(yearBucket -> new YearSummary(yearBucket.getYear(),
                        yearBucket.getMonths().stream()
                                .sorted(Comparator.comparingInt(MonthlyWorkload::getMonth))
                                .map(monthly -> new MonthSummary(
                                        monthly.getMonth(), monthly.getTrainingSummaryDuration()))
                                .toList()))
                .toList();

        return new TrainerWorkloadSummaryResponse(
                document.getTrainerUsername(),
                document.getTrainerFirstName(),
                document.getTrainerLastName(),
                TrainerStatus.of(document.isTrainerStatus()),
                years);
    }

    public MonthlyWorkloadResponse getMonthlyWorkload(String trainerUsername, int year, int month) {
        TrainerWorkloadDocument document = requireTrainer(trainerUsername);
        log.debug("Fetching {}-{} workload for trainer '{}'", year, month, trainerUsername);
        return new MonthlyWorkloadResponse(trainerUsername, year, month, durationOf(document, year, month));
    }

    private void add(TrainerWorkloadDocument document, int year, int month, int duration) {
        YearWorkload yearBucket = findYear(document, year).orElseGet(() -> {
            YearWorkload created = new YearWorkload(year, new ArrayList<>());
            document.getYears().add(created);
            return created;
        });

        findMonth(yearBucket, month).ifPresentOrElse(
                monthly -> monthly.setTrainingSummaryDuration(
                        monthly.getTrainingSummaryDuration() + duration),
                () -> yearBucket.getMonths().add(new MonthlyWorkload(month, duration)));
    }

    private void subtract(TrainerWorkloadDocument document, int year, int month, int duration) {
        Optional<YearWorkload> yearBucket = findYear(document, year);
        Optional<MonthlyWorkload> monthly = yearBucket.flatMap(bucket -> findMonth(bucket, month));
        if (monthly.isEmpty()) {
            log.warn("Cancelling {} minutes for trainer '{}' in {}-{}, but no workload is recorded for that month",
                    duration, document.getTrainerUsername(), year, month);
            return;
        }

        int recorded = monthly.get().getTrainingSummaryDuration();
        int remaining = recorded - duration;
        if (remaining < 0) {
            log.warn("Cancelling {} minutes for trainer '{}' in {}-{} exceeds the recorded {} minutes; clamping to 0",
                    duration, document.getTrainerUsername(), year, month, recorded);
            remaining = 0;
        }

        if (remaining > 0) {
            monthly.get().setTrainingSummaryDuration(remaining);
            return;
        }

        // An emptied month leaves an empty year behind, which would still show up in the summary.
        yearBucket.get().getMonths().removeIf(candidate -> candidate.getMonth() == month);
        if (yearBucket.get().getMonths().isEmpty()) {
            document.getYears().removeIf(candidate -> candidate.getYear() == year);
        }
    }

    private static Optional<YearWorkload> findYear(TrainerWorkloadDocument document, int year) {
        return document.getYears().stream()
                .filter(yearBucket -> yearBucket.getYear() == year)
                .findFirst();
    }

    private static Optional<MonthlyWorkload> findMonth(YearWorkload yearBucket, int month) {
        return yearBucket.getMonths().stream()
                .filter(monthly -> monthly.getMonth() == month)
                .findFirst();
    }

    private static int durationOf(TrainerWorkloadDocument document, int year, int month) {
        return findYear(document, year)
                .flatMap(yearBucket -> findMonth(yearBucket, month))
                .map(MonthlyWorkload::getTrainingSummaryDuration)
                .orElse(0);
    }

    private TrainerWorkloadDocument requireTrainer(String trainerUsername) {
        return repository.findByUsername(trainerUsername)
                .orElseThrow(() -> new TrainerNotFoundException(
                        "No workload recorded for trainer: " + trainerUsername));
    }

    private static TrainerWorkloadDocument newDocument(String trainerUsername) {
        TrainerWorkloadDocument document = new TrainerWorkloadDocument();
        document.setTrainerUsername(trainerUsername);
        document.setYears(new ArrayList<>());
        return document;
    }
}
