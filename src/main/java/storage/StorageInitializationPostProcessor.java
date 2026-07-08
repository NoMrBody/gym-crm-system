package storage;

import model.Trainee;
import model.Trainer;
import model.TrainingType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.util.Map;
import java.util.function.Consumer;

@Component
public class StorageInitializationPostProcessor implements BeanPostProcessor {

    private static final Logger log = LoggerFactory.getLogger(StorageInitializationPostProcessor.class);

    @Value("${data.trainingType.filepath}")
    private String trainingTypeFilePath;

    @Value("${data.trainee.filepath}")
    private String traineeFilePath;

    @Value("${data.trainer.filepath}")
    private String trainerFilePath;

    @Override
    @SuppressWarnings("unchecked")
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        switch (beanName) {
            case "trainingTypeStorage" -> loadTrainingTypes((Map<Long, TrainingType>) bean);
            case "traineeStorage" -> loadTrainees((Map<Long, Trainee>) bean);
            case "trainerStorage" -> loadTrainers((Map<Long, Trainer>) bean);
            default -> {
            }
        }
        return bean;
    }

    private void loadTrainingTypes(Map<Long, TrainingType> storage) {
        readFile(trainingTypeFilePath, parts -> {
            if (parts.length == 2) {
                TrainingType type = new TrainingType();
                type.setId(Long.parseLong(parts[0].trim()));
                type.setTrainingTypeName(parts[1].trim());
                storage.put(type.getId(), type);
                log.info("Loaded TrainingType from file: ID={}, Name={}", type.getId(), type.getTrainingTypeName());
            }
        });
    }

    private void loadTrainees(Map<Long, Trainee> storage) {
        readFile(traineeFilePath, parts -> {
            if (parts.length == 8) {
                Trainee trainee = new Trainee();
                trainee.setUserId(Long.parseLong(parts[0].trim()));
                trainee.setFirstName(parts[1].trim());
                trainee.setLastName(parts[2].trim());
                trainee.setUsername(parts[3].trim());
                trainee.setPassword(parts[4].trim());
                trainee.setActive(Boolean.parseBoolean(parts[5].trim()));
                trainee.setDateOfBirth(LocalDate.parse(parts[6].trim()));
                trainee.setAddress(parts[7].trim());
                storage.put(trainee.getUserId(), trainee);
                log.info("Loaded Trainee from file: ID={}, Username={}", trainee.getUserId(), trainee.getUsername());
            }
        });
    }

    private void loadTrainers(Map<Long, Trainer> storage) {
        readFile(trainerFilePath, parts -> {
            if (parts.length == 7) {
                Trainer trainer = new Trainer();
                trainer.setUserId(Long.parseLong(parts[0].trim()));
                trainer.setFirstName(parts[1].trim());
                trainer.setLastName(parts[2].trim());
                trainer.setUsername(parts[3].trim());
                trainer.setPassword(parts[4].trim());
                trainer.setActive(Boolean.parseBoolean(parts[5].trim()));
                trainer.setSpecialization(parts[6].trim());
                storage.put(trainer.getUserId(), trainer);
                log.info("Loaded Trainer from file: ID={}, Username={}", trainer.getUserId(), trainer.getUsername());
            }
        });
    }

    private void readFile(String filePath, Consumer<String[]> lineHandler) {
        log.info("Initializing storage from file: {}", filePath);
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(filePath)) {
            if (is == null) {
                log.warn("Initialization file not found at path: {}", filePath);
                return;
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isBlank()) {
                        continue;
                    }
                    lineHandler.accept(line.split(","));
                }
            }
        } catch (Exception e) {
            log.error("Failed to load data from file: {}", filePath, e);
        }
    }
}
