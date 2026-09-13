package com.gymcrm.workload.config;

import com.gymcrm.workload.dto.TrainerWorkloadRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.support.converter.JacksonJsonMessageConverter;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.MessageType;

import java.util.Map;

/**
 * JSON text messages matching gym-crm-core. {@code _type} is a logical name, not a Java
 * FQCN, so each service can keep its own request record.
 */
@Configuration
public class JmsConfig {

    public static final String TRANSACTION_ID_PROPERTY = "transactionId";

    static final String TYPE_ID = "TrainerWorkloadRequest";

    @Bean
    public MessageConverter jacksonJmsMessageConverter() {
        JacksonJsonMessageConverter converter = new JacksonJsonMessageConverter();
        converter.setTargetType(MessageType.TEXT);
        converter.setTypeIdPropertyName("_type");
        converter.setTypeIdMappings(Map.of(TYPE_ID, TrainerWorkloadRequest.class));
        converter.setTrustedPackages("com.gymcrm.workload.dto");
        return converter;
    }
}
