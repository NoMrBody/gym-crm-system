package com.gymcrm.workload.dto;

import java.util.List;

/** The trainer's monthly summary, grouped by year then month. */
public record TrainerWorkloadSummaryResponse(
        String trainerUsername,
        String trainerFirstName,
        String trainerLastName,
        TrainerStatus trainerStatus,
        List<YearSummary> years
) {

    public enum TrainerStatus {
        ACTIVE,
        INACTIVE;

        public static TrainerStatus of(boolean active) {
            return active ? ACTIVE : INACTIVE;
        }
    }

    public record YearSummary(int year, List<MonthSummary> months) {
    }

    public record MonthSummary(int month, int trainingSummaryDuration) {
    }
}
