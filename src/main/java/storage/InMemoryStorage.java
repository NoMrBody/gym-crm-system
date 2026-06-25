package storage;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class InMemoryStorage {

    private final Map<String, Map<Long, Object>> storage = new HashMap<>();

    public InMemoryStorage() {
        // pre-initialize
        storage.put("TRAINEE", new HashMap<>());
        storage.put("TRAINER", new HashMap<>());
        storage.put("TRAINING", new HashMap<>());
        storage.put("TRAINING_TYPE", new HashMap<>());
    }

    // returns the entire storage map
    public Map<String, Map<Long, Object>> getStorage() {
        return storage;
    }
}
