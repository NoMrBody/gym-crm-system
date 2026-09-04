package workload;

import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.util.StringUtils;
import org.springframework.web.client.support.RestClientHttpServiceGroupConfigurer;
import org.springframework.web.service.registry.ImportHttpServices;

/**
 * Registers {@link TrainerWorkloadClient} as an HTTP service group. The group's base URL comes
 * from {@code spring.http.serviceclient.trainerWorkload.base-url}; its {@code lb://} scheme makes
 * Spring Cloud LoadBalancer resolve the instance through Eureka.
 */
@Configuration
@ImportHttpServices(group = WorkloadClientConfig.GROUP, types = TrainerWorkloadClient.class)
public class WorkloadClientConfig {

    static final String GROUP = "trainerWorkload";

    /** Correlates the log lines of both services; trainer-workload-service reuses the value. */
    static final String TRANSACTION_HEADER = "X-Transaction-Id";

    private static final String TRANSACTION_ID_MDC_KEY = "transactionId";

    @Bean
    public RestClientHttpServiceGroupConfigurer workloadClientConfigurer(ServiceTokenProvider tokenProvider) {
        return groups -> groups.filterByName(GROUP)
                .forEachClient((group, builder) -> builder.requestInterceptor(authAndTracing(tokenProvider)));
    }

    private static ClientHttpRequestInterceptor authAndTracing(ServiceTokenProvider tokenProvider) {
        return (request, body, execution) -> {
            request.getHeaders().setBearerAuth(tokenProvider.currentToken());

            String transactionId = MDC.get(TRANSACTION_ID_MDC_KEY);
            if (StringUtils.hasText(transactionId)) {
                request.getHeaders().set(TRANSACTION_HEADER, transactionId);
            }

            return execution.execute(request, body);
        };
    }
}
