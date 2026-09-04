package com.gymcrm.workload.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** One (year, month) bucket holding the summed training duration in minutes. */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyWorkload {

    // YEAR and MONTH are reserved words in several databases, hence the explicit names.
    @Column(name = "training_year", nullable = false)
    private int trainingYear;

    @Column(name = "training_month", nullable = false)
    private int trainingMonth;

    @Column(name = "total_duration", nullable = false)
    private int totalDuration;

    public boolean isFor(int year, int month) {
        return this.trainingYear == year && this.trainingMonth == month;
    }
}
