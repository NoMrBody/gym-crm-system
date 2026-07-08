package storage;

import model.Trainee;
import model.Trainer;
import model.TrainingType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class StorageInitializationPostProcessorTest {

    private StorageInitializationPostProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new StorageInitializationPostProcessor();
        ReflectionTestUtils.setField(processor, "trainingTypeFilePath", "test-trainingTypes.csv");
        ReflectionTestUtils.setField(processor, "traineeFilePath", "test-trainees.csv");
        ReflectionTestUtils.setField(processor, "trainerFilePath", "test-trainers.csv");
    }

    @Test
    void loadsTrainingTypes_intoTrainingTypeStorageBean() {
        Map<Long, TrainingType> storage = new HashMap<>();

        Object result = processor.postProcessAfterInitialization(storage, "trainingTypeStorage");

        assertSame(storage, result);
        assertEquals(2, storage.size());
        assertEquals("TestCardio", storage.get(10L).getTrainingTypeName());
        assertEquals("TestStrength", storage.get(20L).getTrainingTypeName());
    }

    @Test
    void loadsTrainees_withParsedDateAndActiveFlag() {
        Map<Long, Trainee> storage = new HashMap<>();

        processor.postProcessAfterInitialization(storage, "traineeStorage");

        assertEquals(1, storage.size());
        Trainee trainee = storage.get(100L);
        assertNotNull(trainee);
        assertEquals("Test.Trainee", trainee.getUsername());
        assertTrue(trainee.isActive());
        assertEquals(LocalDate.of(1995, 6, 15), trainee.getDateOfBirth());
        assertEquals("1 Test Street", trainee.getAddress());
    }

    @Test
    void loadsTrainers_withSpecializationAndActiveFlag() {
        Map<Long, Trainer> storage = new HashMap<>();

        processor.postProcessAfterInitialization(storage, "trainerStorage");

        assertEquals(1, storage.size());
        Trainer trainer = storage.get(200L);
        assertNotNull(trainer);
        assertEquals("Test.Trainer", trainer.getUsername());
        assertFalse(trainer.isActive());
        assertEquals("Boxing", trainer.getSpecialization());
    }

    @Test
    void unknownBeanName_leavesBeanUntouched() {
        Map<Long, Object> storage = new HashMap<>();

        Object result = processor.postProcessAfterInitialization(storage, "someOtherBean");

        assertSame(storage, result);
        assertTrue(storage.isEmpty());
    }
}
