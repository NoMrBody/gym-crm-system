package com.gymcrm.workload.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

/** A trainer and the monthly totals of the trainings reported for them. */
@Document(collection = "trainer_workloads")
@CompoundIndex(name = "idx_trainer_name", def = "{'trainerFirstName': 1, 'trainerLastName': 1}")
@Getter
@Setter
@NoArgsConstructor
public class TrainerWorkloadDocument {

    @Id
    private String trainerUsername;

    @NotBlank
    private String trainerFirstName;

    @NotBlank
    private String trainerLastName;

    private boolean trainerStatus;

    @Valid
    private List<YearWorkload> years = new ArrayList<>();
}
