package com.gymcrm.workload.repository;

import com.gymcrm.workload.model.TrainerWorkloadDocument;
import com.mongodb.client.result.UpdateResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.mongodb.core.query.Query;

import java.util.Optional;

public class TrainerWorkloadRepositoryCustomImpl implements TrainerWorkloadRepositoryCustom{

    private static final Logger log = LoggerFactory.getLogger(TrainerWorkloadRepositoryCustomImpl.class);
    private final MongoTemplate mongoTemplate;

    public TrainerWorkloadRepositoryCustomImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Optional<TrainerWorkloadDocument> findByUsername(String trainerUsername) {
        Query query = Query.query(Criteria.where("_id").is(trainerUsername));
        log.debug("Mongo findOne on trainer_workloads by _id '{}'", trainerUsername);
        TrainerWorkloadDocument found = mongoTemplate.findOne(query, TrainerWorkloadDocument.class);
        log.debug("Mongo findOne by _id '{}' {}", trainerUsername, found == null ? "found nothing" : "found a document");
        return Optional.ofNullable(found);
    }

    @Override
    public void upsertByUsername(TrainerWorkloadDocument document) {
        Query query = Query.query(Criteria.where("_id").is(document.getTrainerUsername()));
        Update update = new Update()
                .set("trainerFirstName", document.getTrainerFirstName())
                .set("trainerLastName", document.getTrainerLastName())
                .set("trainerStatus", document.isTrainerStatus())
                .set("years", document.getYears());
        log.debug("Mongo upsert on trainer_workloads by _id '{}'", document.getTrainerUsername());
        UpdateResult result = mongoTemplate.upsert(query, update, TrainerWorkloadDocument.class);
        log.debug("Mongo upsert by _id '{}': matched={} modified={} upsertedId={}",
                document.getTrainerUsername(), result.getMatchedCount(), result.getModifiedCount(),
                result.getUpsertedId());
    }
}
