package workload;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.support.converter.JacksonJsonMessageConverter;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.MessageType;

import java.util.Map;

/**
 * JSON text messages so the ActiveMQ console is readable. {@code _type} carries a logical
 * name rather than a Java FQCN, because each service keeps its own request record.
 */
@Configuration
public class JmsConfig {

    static final String TRANSACTION_ID_PROPERTY = "transactionId";

    static final String TYPE_ID = "TrainerWorkloadRequest";

    @Bean
    public MessageConverter jacksonJmsMessageConverter() {
        JacksonJsonMessageConverter converter = new JacksonJsonMessageConverter();
        converter.setTargetType(MessageType.TEXT);
        converter.setTypeIdPropertyName("_type");
        converter.setTypeIdMappings(Map.of(TYPE_ID, TrainerWorkloadRequest.class));
        converter.setTrustedPackages("workload");
        return converter;
    }
}
