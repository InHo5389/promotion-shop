package orderservice.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import event.publisher.EventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import outboxmessagerelay.OutboxEventPublisher;

@Configuration
@RequiredArgsConstructor
public class EventPublisherConfig {

    private final ApplicationEventPublisher applicationEventPublisher;
    private final ObjectMapper objectMapper;

    @Bean
    public EventPublisher eventPublisher(KafkaTemplate<String, String> kafkaTemplate) {
        return new EventPublisher(kafkaTemplate);
    }

    @Bean
    public OutboxEventPublisher outboxEventPublisher(){
        return new OutboxEventPublisher(applicationEventPublisher,objectMapper);
    }
}
