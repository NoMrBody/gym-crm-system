package bdd;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;
import workload.TrainerWorkloadRequest;

@Component
public class CoreMessagingSupport {

    private final JmsTemplate jmsTemplate;
    private final String queue;

    public CoreMessagingSupport(JmsTemplate jmsTemplate,
                                @Value("${gym.workload.queue}") String queue) {
        this.jmsTemplate = jmsTemplate;
        this.queue = queue;
    }

    public void drainQueue() {
        long previous = jmsTemplate.getReceiveTimeout();
        jmsTemplate.setReceiveTimeout(50);
        try {
            while (jmsTemplate.receive(queue) != null) {
                // drop leftover messages from earlier scenarios
            }
        } finally {
            jmsTemplate.setReceiveTimeout(previous);
        }
    }

    public TrainerWorkloadRequest receiveEvent(long timeoutMs) {
        long previous = jmsTemplate.getReceiveTimeout();
        jmsTemplate.setReceiveTimeout(timeoutMs);
        try {
            Object payload = jmsTemplate.receiveAndConvert(queue);
            if (payload == null) {
                return null;
            }
            return (TrainerWorkloadRequest) payload;
        } finally {
            jmsTemplate.setReceiveTimeout(previous);
        }
    }
}
