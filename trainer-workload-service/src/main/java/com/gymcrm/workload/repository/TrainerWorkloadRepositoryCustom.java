package com.gymcrm.workload.repository;

import com.gymcrm.workload.model.TrainerWorkloadDocument;

import java.util.Optional;

public interface TrainerWorkloadRepositoryCustom {
    Optional<TrainerWorkloadDocument> findByUsername(String trainerUsername);
    void upsertByUsername(TrainerWorkloadDocument document);
}
