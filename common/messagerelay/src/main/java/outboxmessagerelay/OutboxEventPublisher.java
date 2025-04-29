package outboxmessagerelay;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import event.Event;
import event.EventPayload;
import event.EventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import outboxmessagerelay.entity.Outbox;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;
    private final ObjectMapper objectMapper;

    /**
     * 쿠폰 발급 요청 메시지를 Outbox 이벤트로 발행
     */
    public void publishCouponIssueRequest(Object message) {
        try {
            String messageJson = objectMapper.writeValueAsString(message);

            Outbox outbox = Outbox.create(
                    "coupon-issue-requests",
                    messageJson
            );

            applicationEventPublisher.publishEvent(OutboxEvent.of(outbox));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize coupon issue message", e);
        }
    }

    public void publish(EventType type, EventPayload payload) {
        log.info("Publishing event: type={}, payload={}", type, payload);
        Outbox outbox = Outbox.create(
                type.getTopic(),
                Event.of(System.currentTimeMillis(), type, payload).toJson()
        );
        log.info("Created outbox: {}", outbox);
        applicationEventPublisher.publishEvent(OutboxEvent.of(outbox));
        log.info("Event published to Spring context");
    }
}
