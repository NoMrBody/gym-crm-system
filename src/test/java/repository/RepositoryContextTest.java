package repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertNotNull;

// Boots the JPA slice so an invalid derived method name or a broken @Query fails here
// rather than at application startup.
// Application lives in the default package and cannot be imported from here, so a
// minimal nested config mirrors its @EntityScan / @EnableJpaRepositories.
@DataJpaTest
@ContextConfiguration(classes = RepositoryContextTest.RepositoryTestConfig.class)
@TestPropertySource(properties = "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect")
class RepositoryContextTest {

    @Configuration
    @EntityScan("model")
    @EnableJpaRepositories("repository")
    static class RepositoryTestConfig {
    }

    @Autowired
    private TraineeRepository traineeRepository;
    @Autowired
    private TrainerRepository trainerRepository;
    @Autowired
    private TrainingRepository trainingRepository;
    @Autowired
    private TrainingTypeRepository trainingTypeRepository;
    @Autowired
    private UserRepository userRepository;

    @Test
    void allRepositoriesAreCreated() {
        assertNotNull(traineeRepository);
        assertNotNull(trainerRepository);
        assertNotNull(trainingRepository);
        assertNotNull(trainingTypeRepository);
        assertNotNull(userRepository);
    }
}
