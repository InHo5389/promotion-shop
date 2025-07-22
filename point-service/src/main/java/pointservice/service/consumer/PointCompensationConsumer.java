package pointservice.service.consumer;

import event.Event;
import event.EventPayload;
import event.EventType;
import event.payload.compensation.CompensationCompletedPayload;
import event.payload.compensation.PointCompensationRequestPayload;
import event.publisher.EventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pointservice.service.v2.RedissonLockPointService;

@Slf4j
@Component
@RequiredArgsConstructor
public class PointCompensationConsumer {

    private final RedissonLockPointService redissonLockPointService;
    private final EventPublisher eventPublisher;

    @KafkaListener(topics = EventType.Topic.POINT_COMPENSATION_REQUEST, groupId = "point-compensation-consumer")
    @Transactional
    public void handleCouponCompensation(String message) {
        log.info("Received coupon compensation request: {}", message);

        try {
            Event<EventPayload> event = Event.fromJson(message);
            PointCompensationRequestPayload payload = (PointCompensationRequestPayload) event.getPayload();

            log.info("Processing point compensation - sagaId: {}, userId: {}, pointAmount: {}P, orderId: {}",
                    payload.getSagaId(), payload.getUserId(), payload.getPointAmount(), payload.getOrderId());

            // 포인트 환급 처리 (사용한 포인트만큼 다시 적립)
            redissonLockPointService.earn(payload.getUserId(), payload.getPointAmount());

            // 성공 이벤트 발행
            publishCompensationCompleted(payload, true, null);

            log.info("Point compensation completed successfully - sagaId: {}, userId: {}, refundAmount: {}P",
                    payload.getSagaId(), payload.getUserId(), payload.getPointAmount());

        } catch (Exception e) {
            log.error("Point compensation failed: {}", e.getMessage(), e);

            try {
                Event<EventPayload> event = Event.fromJson(message);
                PointCompensationRequestPayload payload = (PointCompensationRequestPayload) event.getPayload();
                publishCompensationCompleted(payload, false, e.getMessage());
            } catch (Exception parseError) {
                log.error("Failed to parse compensation event for error handling: {}", parseError.getMessage());
            }
        }
    }

    private void publishCompensationCompleted(PointCompensationRequestPayload originalPayload,
                                              boolean success, String errorMessage) {
        CompensationCompletedPayload completedPayload = CompensationCompletedPayload.builder()
                .sagaId(originalPayload.getSagaId())
                .orderId(originalPayload.getOrderId())
                .userId(originalPayload.getUserId())
                .compensationType("POINT_REFUND")
                .success(success)
                .errorMessage(errorMessage)
                .timestamp(System.currentTimeMillis())
                .build();

        eventPublisher.publishEvent(
                EventType.COMPENSATION_COMPLETED,
                completedPayload,
                EventType.Topic.COMPENSATION_COMPLETED
        );

        log.info("Point compensation completed event published - sagaId: {}, success: {}",
                originalPayload.getSagaId(), success);
    }
}