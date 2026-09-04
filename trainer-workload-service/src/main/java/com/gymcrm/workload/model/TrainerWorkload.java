package com.gymcrm.workload.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/** A trainer and the monthly totals of the trainings reported for them. */
@Entity
@Table(name = "trainer_workloads")
@Getter
@Setter
@NoArgsConstructor
public class TrainerWorkload {

    @Id
    @Column(name = "trainer_username", nullable = false)
    private String trainerUsername;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "trainer_monthly_workloads",
            joinColumns = @JoinColumn(name = "trainer_username"))
    private List<MonthlyWorkload> monthlyWorkloads = new ArrayList<>();
}
