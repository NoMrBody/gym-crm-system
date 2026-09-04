package com.gymcrm.workload.dto;

/** The summed duration for one trainer in one specific month. */
public record MonthlyWorkloadResponse(
        String trainerUsername,
        int year,
        int month,
        int trainingSummaryDuration
) {
}
