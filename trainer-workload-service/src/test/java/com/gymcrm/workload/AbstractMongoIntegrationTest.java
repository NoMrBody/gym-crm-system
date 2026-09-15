package com.gymcrm.workload;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mongodb.MongoDBContainer;

/**
 * One MongoDB container for every context-based test. The field is static, so the
 * container starts once per JVM and the Spring context stays cacheable across subclasses.
 *
 * <p>{@code @ServiceConnection} feeds the container's URI to the context, which is why no
 * {@code spring.mongodb.uri} is set in the test properties. A standalone server is enough:
 * one trainer is one document, so nothing here needs a replica set.
 */
@Testcontainers
public abstract class AbstractMongoIntegrationTest {

    @Container
    @ServiceConnection
    public static final MongoDBContainer MONGO = new MongoDBContainer("mongo:8")
            // MongoDB 8.0+ refuses to start on Linux kernels 6.19 through 7.0.13
            // (SERVER-121912, a vendored TCMalloc/rseq incompatibility).
            .withEnv("GLIBC_TUNABLES", "glibc.pthread.rseq=1");
}
