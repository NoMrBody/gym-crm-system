package storage;

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
import java.util.Map;

@Component
public class StorageInitializationPostProcessor implements BeanPostProcessor {

    private static final Logger log = LoggerFactory.getLogger(StorageInitializationPostProcessor.class);

    @Value("${data.filepath}")
    private String filePath;

    @Override
    @SuppressWarnings("unchecked")
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if ("trainingTypeStorage".equals(beanName)) {
            log.info("Intercepted trainingTypeStorage bean. Initializing data from file: {}", filePath);
            loadDataFromFile((Map<Long, TrainingType>) bean);
        }
        return bean;
    }

    private void loadDataFromFile(Map<Long, TrainingType> storage) {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(filePath)) {
            if (is == null) {
                log.warn("Initialization file not found at path: {}", filePath);
                return;
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split(",");
                    if (parts.length == 2) {
                        Long id = Long.parseLong(parts[0].trim());
                        String name = parts[1].trim();

                        TrainingType type = new TrainingType();
                        type.setId(id);
                        type.setTrainingTypeName(name);

                        storage.put(id, type);
                        log.info("Loaded TrainingType from file: ID={}, Name={}", id, name);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to load data from file: ", e);
        }
    }
}
