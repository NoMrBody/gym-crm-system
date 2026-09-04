package workload;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

/**
 * Declarative client for trainer-workload-service. The implementation is generated from this
 * interface; see {@link WorkloadClientConfig} for the base URL and the outgoing headers.
 */
@HttpExchange("/api/v1/trainer-workloads")
public interface TrainerWorkloadClient {

    @PostExchange
    void submitWorkload(@RequestBody TrainerWorkloadRequest request);
}
