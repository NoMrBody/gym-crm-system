package bdd;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Test copy of {@code Application} so Cucumber can live in package {@code bdd}.
 * The production main class is in the default package and cannot be referenced from here.
 */
@SpringBootApplication(scanBasePackages = {
        "config", "controller", "exception", "facade", "mapper", "service", "util", "metrics", "health", "security",
        "workload", "bdd"
})
@EntityScan("model")
@EnableJpaRepositories("repository")
@EnableScheduling
public class GymCrmComponentTestApp {
}
