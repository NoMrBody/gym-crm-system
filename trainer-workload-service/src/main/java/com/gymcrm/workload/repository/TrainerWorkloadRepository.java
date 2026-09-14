package com.gymcrm.workload.repository;

import com.gymcrm.workload.model.TrainerWorkloadDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface TrainerWorkloadRepository
        extends MongoRepository<TrainerWorkloadDocument, String>, TrainerWorkloadRepositoryCustom {
    List<TrainerWorkloadDocument> findByTrainerFirstNameAndTrainerLastName(String firstName, String lastName);

}
