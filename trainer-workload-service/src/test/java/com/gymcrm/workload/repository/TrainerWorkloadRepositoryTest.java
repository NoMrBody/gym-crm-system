package com.gymcrm.workload.repository;

import com.gymcrm.workload.AbstractMongoIntegrationTest;
import com.gymcrm.workload.model.MonthlyWorkload;
import com.gymcrm.workload.model.TrainerWorkloadDocument;
import com.gymcrm.workload.model.YearWorkload;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.IndexInfo;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Mongo-only slice: proves the mapping, the index and the custom fragment against a real
 * server. Each test uses its own trainer so the shared container needs no cleanup.
 */
@DataMongoTest
@ActiveProfiles("test")
class TrainerWorkloadRepositoryTest extends AbstractMongoIntegrationTest {

    @Autowired
    private TrainerWorkloadRepository repository;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Test
    void theCompoundIndexOnTheTrainerNameIsCreated() {
        List<String> indexNames = mongoTemplate.indexOps(TrainerWorkloadDocument.class).getIndexInfo()
                .stream()
                .map(IndexInfo::getName)
                .toList();

        assertTrue(indexNames.contains("idx_trainer_name"),
                "expected idx_trainer_name among " + indexNames);
    }

    @Test
    void findByTrainerFirstNameAndTrainerLastName_returnsTheTrainer() {
        repository.save(document("Search.Trainer", "Search", "Target", true, 2026, 3, 60));

        List<TrainerWorkloadDocument> found =
                repository.findByTrainerFirstNameAndTrainerLastName("Search", "Target");

        assertEquals(1, found.size());
        assertEquals("Search.Trainer", found.getFirst().getTrainerUsername());
    }

    @Test
    void findByUsername_returnsTheSavedDocument() {
        repository.save(document("Found.Trainer", "Found", "Trainer", false, 2026, 5, 90));

        Optional<TrainerWorkloadDocument> found = repository.findByUsername("Found.Trainer");

        assertTrue(found.isPresent());
        assertEquals("Found", found.get().getTrainerFirstName());
        assertEquals(90, found.get().getYears().getFirst().getMonths().getFirst()
                .getTrainingSummaryDuration());
    }

    @Test
    void findByUsername_returnsEmptyForAnUnknownTrainer() {
        assertTrue(repository.findByUsername("Nobody.Here").isEmpty());
    }

    @Test
    void upsertByUsername_writesTheNestedDocumentShape() {
        repository.upsertByUsername(document("Shape.Trainer", "Shape", "Checker", true, 2026, 3, 60));

        Document raw = mongoTemplate
                .getCollection(mongoTemplate.getCollectionName(TrainerWorkloadDocument.class))
                .find(new Document("_id", "Shape.Trainer"))
                .first();

        assertNotNull(raw, "the upsert should have created the document");
        assertEquals("Shape", raw.getString("trainerFirstName"));
        assertEquals("Checker", raw.getString("trainerLastName"));
        // The assignment requires a BSON boolean here, not a string or a number.
        assertInstanceOf(Boolean.class, raw.get("trainerStatus"));
        assertEquals(Boolean.TRUE, raw.get("trainerStatus"));

        List<Document> years = raw.getList("years", Document.class);
        assertEquals(1, years.size());
        assertEquals(2026, years.getFirst().getInteger("year"));

        List<Document> months = years.getFirst().getList("months", Document.class);
        assertEquals(1, months.size());
        assertEquals(3, months.getFirst().getInteger("month"));
        // And a number here, so getInteger would fail if it had been stored as a double.
        assertInstanceOf(Integer.class, months.getFirst().get("trainingSummaryDuration"));
        assertEquals(60, months.getFirst().getInteger("trainingSummaryDuration"));
    }

    @Test
    void upsertByUsername_replacesTheProfileOfAnExistingDocument() {
        repository.save(document("Refresh.Trainer", "Old", "Name", true, 2026, 3, 60));

        repository.upsertByUsername(document("Refresh.Trainer", "New", "Name", false, 2026, 3, 75));

        TrainerWorkloadDocument reloaded = repository.findByUsername("Refresh.Trainer").orElseThrow();
        assertEquals("New", reloaded.getTrainerFirstName());
        assertEquals(false, reloaded.isTrainerStatus());
        assertEquals(75, reloaded.getYears().getFirst().getMonths().getFirst()
                .getTrainingSummaryDuration());
    }

    private static TrainerWorkloadDocument document(String username, String firstName, String lastName,
                                                    boolean status, int year, int month, int duration) {
        TrainerWorkloadDocument document = new TrainerWorkloadDocument();
        document.setTrainerUsername(username);
        document.setTrainerFirstName(firstName);
        document.setTrainerLastName(lastName);
        document.setTrainerStatus(status);

        List<MonthlyWorkload> months = new ArrayList<>(List.of(new MonthlyWorkload(month, duration)));
        document.setYears(new ArrayList<>(List.of(new YearWorkload(year, months))));
        return document;
    }
}
