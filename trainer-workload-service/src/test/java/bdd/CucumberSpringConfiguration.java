package bdd;

import com.gymcrm.workload.AbstractMongoIntegrationTest;
import com.gymcrm.workload.TrainerWorkloadApplication;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.mongodb.MongoDBContainer;

@CucumberContextConfiguration
@SpringBootTest(classes = TrainerWorkloadApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({
        WorkloadWorld.class,
        WorkloadHttpSupport.class,
        WorkloadMessagingSupport.class,
        TestJwtTokens.class
})
public class CucumberSpringConfiguration {

    private static final String LOCAL_MONGO_URI = "mongodb://127.0.0.1:27017/trainer_workload_cucumber";


    @DynamicPropertySource
    static void mongoUri(DynamicPropertyRegistry registry) {
        registry.add("spring.mongodb.uri", CucumberSpringConfiguration::resolveMongoUri);
    }

    private static String resolveMongoUri() {
        try {
            MongoDBContainer mongo = AbstractMongoIntegrationTest.MONGO;
            if (!mongo.isRunning()) {
                mongo.start();
            }
            return mongo.getConnectionString() + "/trainer_workload";
        } catch (Exception ex) {
            return LOCAL_MONGO_URI;
        }
    }
}
