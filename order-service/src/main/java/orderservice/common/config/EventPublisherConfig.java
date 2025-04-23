package orderservice.common.config;

import event.publisher.EventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;

@Configuration
public class EventPublisherConfig {

    @Bean
    public EventPublisher eventPublisher(KafkaTemplate<String, String> kafkaTemplate) {
        return new EventPublisher(kafkaTemplate);
    }
}
