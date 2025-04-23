package event.publisher;

import event.Event;
import event.EventPayload;
import event.EventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;

@Slf4j
@RequiredArgsConstructor
public class EventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;

    private Long generateEventId() {
        return System.currentTimeMillis();
    }

    public void publishEvent(EventType eventType, EventPayload payload, String topic) {
        Event<EventPayload> event = Event.of(generateEventId(), eventType, payload);
        String eventJson = event.toJson();
        log.info("Event published: id={}, type={}, topic={}", event.getEventId(), eventType, topic);

        kafkaTemplate.send(topic, eventJson);
    }
}
