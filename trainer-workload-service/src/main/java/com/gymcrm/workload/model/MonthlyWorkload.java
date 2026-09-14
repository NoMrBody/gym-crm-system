package com.gymcrm.workload.model;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyWorkload {

    @Min(1)
    @Max(12)
    private int month;

    @PositiveOrZero
    private int trainingSummaryDuration;
}
