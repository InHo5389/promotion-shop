package couponservice.service.v3.consumer;

import couponservice.service.v3.CouponService;
import event.Event;
import event.EventPayload;
import event.EventType;
import event.payload.compensation.CompensationCompletedPayload;
import event.payload.compensation.CouponCompensationRequestPayload;
import event.publisher.EventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class CouponCompensationConsumer {

    private final CouponService couponService;
    private final EventPublisher eventPublisher;

    @KafkaListener(topics = EventType.Topic.COUPON_COMPENSATION_REQUEST, groupId = "coupon-compensation-consumer")
    @Transactional
    public void handleCouponCompensation(String message) {
        log.info("Received coupon compensation request: {}", message);

        try {
            Event<EventPayload> event = Event.fromJson(message);
            CouponCompensationRequestPayload payload = (CouponCompensationRequestPayload) event.getPayload();

            log.info("Processing coupon compensation - sagaId: {}, couponId: {}, orderId: {}",
                    payload.getSagaId(), payload.getCouponId(), payload.getOrderId());


            // 쿠폰 취소 처리 (사용 상태를 취소로 변경)
            couponService.cancel(payload.getCouponId());

            // 성공 이벤트 발행
            publishCompensationCompleted(payload, true, null);

            log.info("Coupon compensation completed successfully - sagaId: {}, couponId: {}",
                    payload.getSagaId(), payload.getCouponId());

        } catch (Exception e) {
            log.error("Coupon compensation failed: {}", e.getMessage(), e);

            try {
                Event<EventPayload> event = Event.fromJson(message);
                CouponCompensationRequestPayload payload = (CouponCompensationRequestPayload) event.getPayload();
                publishCompensationCompleted(payload, false, e.getMessage());
            } catch (Exception parseError) {
                log.error("Failed to parse compensation event for error handling: {}", parseError.getMessage());
            }
        }
    }

    private void publishCompensationCompleted(CouponCompensationRequestPayload originalPayload,
                                              boolean success, String errorMessage) {
        CompensationCompletedPayload completedPayload = CompensationCompletedPayload.builder()
                .sagaId(originalPayload.getSagaId())
                .orderId(originalPayload.getOrderId())
                .userId(originalPayload.getUserId())
                .compensationType("COUPON_CANCEL")
                .success(success)
                .errorMessage(errorMessage)
                .timestamp(System.currentTimeMillis())
                .build();

        eventPublisher.publishEvent(
                EventType.COMPENSATION_COMPLETED,
                completedPayload,
                EventType.Topic.COMPENSATION_COMPLETED
        );

        log.info("Coupon compensation completed event published - sagaId: {}, success: {}",
                originalPayload.getSagaId(), success);
    }
}