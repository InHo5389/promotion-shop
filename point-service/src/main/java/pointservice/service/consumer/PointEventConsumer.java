package pointservice.service.consumer;

import event.Event;
import event.EventPayload;
import event.EventType;
import event.payload.PointEarnedEventPayload;
import event.payload.PointUsedEventPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import pointservice.service.v2.RedissonLockPointService;

@Slf4j
@Component
@RequiredArgsConstructor
public class PointEventConsumer {

    private final RedissonLockPointService redissonLockPointService;

    @KafkaListener(
            topics = EventType.Topic.POINT_USED,
            groupId = "point-used-consumer"
    )
    public void consumeUsedPoint(String message) {
        log.info("[PointEventConsumer] Received message: {}", message);

        Event<EventPayload> event = Event.fromJson(message);

        PointUsedEventPayload payload = (PointUsedEventPayload) event.getPayload();
        log.info("Processing use point - orderId: {}", payload.getOrderId());

        try {
            redissonLockPointService.use(payload.getUserId(), payload.getPointBalance());
            log.info("Point used successfully for orderId: {}", payload.getOrderId());
        } catch (Exception e) {
            log.error("Failed to use point for orderId: {}", payload.getOrderId(), e);
        }
    }

    @KafkaListener(
            topics = EventType.Topic.POINT_CANCELED,
            groupId = "point-canceled-consumer"
    )
    public void consumeCanceledPoint(String message) {
        log.info("[PointEventConsumer] Received message: {}", message);

        Event<EventPayload> event = Event.fromJson(message);

        PointEarnedEventPayload payload = (PointEarnedEventPayload) event.getPayload();
        log.info("Processing cancel point - orderId: {}", payload.getOrderId());

        try {
            redissonLockPointService.earn(payload.getUserId(), payload.getPointBalance());
            log.info("Point canceled successfully for orderId: {}", payload.getOrderId());
        } catch (Exception e) {
            log.error("Failed to cancel point for orderId: {}", payload.getOrderId(), e);
        }
    }
}
