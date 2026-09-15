package bdd;

import com.gymcrm.workload.TrainerWorkloadApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.mongodb.MongoDBContainer;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Boots gym-crm-core and trainer-workload-service in this JVM against a shared in-memory
 * ActiveMQ broker and H2. MongoDB is a Testcontainers instance when Docker is available,
 * otherwise a local MongoDB on port 27017.
 */
public final class DualAppSupport {

    static final String JWT_SECRET = "integration-test-secret-key-that-is-long-enough";
    static final String JWT_ISSUER = "gym-crm";
    static final String QUEUE = "gym.trainer.workload";
    static final String BROKER_URL = "vm://localhost?broker.persistent=false";

    private static final String CORE_MONGO_EXCLUDES = String.join(",",
            "org.springframework.boot.mongodb.autoconfigure.MongoAutoConfiguration",
            "org.springframework.boot.data.mongodb.autoconfigure.DataMongoAutoConfiguration",
            "org.springframework.boot.data.mongodb.autoconfigure.DataMongoRepositoriesAutoConfiguration",
            "org.springframework.boot.mongodb.autoconfigure.health.MongoHealthContributorAutoConfiguration",
            "org.springframework.boot.mongodb.autoconfigure.metrics.MongoMetricsAutoConfiguration");

    private static final String WORKLOAD_JPA_EXCLUDES = String.join(",",
            "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration",
            "org.springframework.boot.jdbc.autoconfigure.DataSourceTransactionManagerAutoConfiguration",
            "org.springframework.boot.jdbc.autoconfigure.JdbcTemplateAutoConfiguration",
            "org.springframework.boot.jdbc.autoconfigure.DataSourceInitializationAutoConfiguration",
            "org.springframework.boot.jdbc.autoconfigure.health.DataSourceHealthContributorAutoConfiguration",
            "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration",
            "org.springframework.boot.hibernate.autoconfigure.metrics.HibernateMetricsAutoConfiguration",
            "org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration");

    private static MongoDBContainer mongo;
    private static String mongoUri;
    private static ConfigurableApplicationContext core;
    private static ConfigurableApplicationContext workload;
    private static String coreBaseUrl;
    private static String workloadBaseUrl;

    private DualAppSupport() {
    }

    public static synchronized void start() {
        if (core != null) {
            return;
        }
        mongo = new MongoDBContainer("mongo:8")
                .withEnv("GLIBC_TUNABLES", "glibc.pthread.rseq=1");
        try {
            mongo.start();
            mongoUri = mongo.getConnectionString() + "/trainer_workload_it";
        } catch (Exception ex) {
            mongo = null;
            mongoUri = "mongodb://127.0.0.1:27017/trainer_workload_it";
        }

        try {
            Class<?> coreApplication = Class.forName("Application");
            core = new SpringApplicationBuilder(coreApplication)
                    .web(WebApplicationType.SERVLET)
                    .run(toArgs(coreProperties()));
            coreBaseUrl = "http://localhost:" + portOf(core);

            workload = new SpringApplicationBuilder(TrainerWorkloadApplication.class)
                    .web(WebApplicationType.SERVLET)
                    .run(toArgs(workloadProperties()));
            workloadBaseUrl = "http://localhost:" + portOf(workload);
        } catch (RuntimeException | ClassNotFoundException ex) {
            stop();
            throw new IllegalStateException("Failed to start the two microservices", ex);
        }
    }

    public static synchronized void stop() {
        if (workload != null) {
            workload.close();
            workload = null;
        }
        if (core != null) {
            core.close();
            core = null;
        }
        if (mongo != null) {
            mongo.stop();
            mongo = null;
        }
    }

    public static String coreBaseUrl() {
        return coreBaseUrl;
    }

    public static String workloadBaseUrl() {
        return workloadBaseUrl;
    }

    private static Map<String, Object> sharedMessagingAndSecurity() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("spring.profiles.active", "it");
        properties.put("server.port", 0);
        properties.put("spring.main.banner-mode", "off");
        properties.put("eureka.client.enabled", false);
        properties.put("eureka.client.register-with-eureka", false);
        properties.put("eureka.client.fetch-registry", false);
        properties.put("spring.cloud.discovery.enabled", false);
        properties.put("spring.activemq.broker-url", BROKER_URL);
        properties.put("gym.workload.queue", QUEUE);
        properties.put("gym.security.jwt.secret", JWT_SECRET);
        properties.put("gym.security.jwt.issuer", JWT_ISSUER);
        properties.put("gym.security.jwt.ttl", "1h");
        return properties;
    }

    private static Map<String, Object> coreProperties() {
        Map<String, Object> properties = sharedMessagingAndSecurity();
        properties.put("spring.application.name", "gym-crm");
        properties.put("spring.autoconfigure.exclude", CORE_MONGO_EXCLUDES);
        properties.put("spring.task.scheduling.enabled", false);
        properties.put("spring.datasource.url", "jdbc:h2:mem:gym-crm-it;DB_CLOSE_DELAY=-1;MODE=PostgreSQL");
        properties.put("spring.datasource.driver-class-name", "org.h2.Driver");
        properties.put("spring.datasource.username", "sa");
        properties.put("spring.datasource.password", "");
        properties.put("spring.jpa.hibernate.ddl-auto", "create-drop");
        properties.put("spring.jpa.properties.hibernate.dialect", "org.hibernate.dialect.H2Dialect");
        properties.put("gym.security.brute-force.max-attempts", 3);
        properties.put("gym.security.brute-force.block-duration", "5m");
        properties.put("gym.security.cors.allowed-origins", "http://localhost:3000");
        return properties;
    }

    private static Map<String, Object> workloadProperties() {
        Map<String, Object> properties = sharedMessagingAndSecurity();
        properties.put("spring.application.name", "trainer-workload-service");
        properties.put("spring.autoconfigure.exclude", WORKLOAD_JPA_EXCLUDES);
        properties.put("spring.mongodb.uri", mongoUri);
        properties.put("spring.data.mongodb.auto-index-creation", true);
        return properties;
    }

    private static String[] toArgs(Map<String, Object> properties) {
        // Command-line args beat application.properties, unlike SpringApplicationBuilder.properties().
        return properties.entrySet().stream()
                .map(entry -> "--" + entry.getKey() + "=" + entry.getValue())
                .toArray(String[]::new);
    }

    private static String portOf(ConfigurableApplicationContext context) {
        return context.getEnvironment().getProperty("local.server.port");
    }
}
