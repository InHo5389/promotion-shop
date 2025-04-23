package couponservice.service.v3;

import couponservice.service.dto.v3.CouponDto;
import event.Event;
import event.EventPayload;
import event.EventType;
import event.payload.CouponAppliedEventPayload;
import event.payload.CouponCanceledEventPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CouponConsumer {

    private final CouponService couponService;

    /**
     * 토픽이 coupon-issue-requests인 경우
     * groupId가 coupon-service인 경우
     * 설정한 factory를 사용해서 해당 메시지를 역직렬화하여 하여 해당 메서드 호출
     */
    @KafkaListener(topics = "coupon-issue-requests", groupId = "coupon-service", containerFactory = "couponKafkaListenerContainerFactory")
    public void consumeCouponIssueRequest(CouponDto.IssueMessage message) {
        try {
            log.info("[CouponConsumer.consumeCouponIssueRequest()] Received message: {}", message);
            couponService.issue(message);
        } catch (Exception e) {
            log.error("Failed to process coupon issue request: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(
            topics = EventType.Topic.COUPON_APPLIED,
            groupId = "coupon-applied-consumer")
    public void consumeCouponApplied(String message) {
        log.info("[CouponConsumer.consumeCouponApplied()] Received message: {}", message);
        Event<EventPayload> event = Event.fromJson(message);

        CouponAppliedEventPayload payload = (CouponAppliedEventPayload) event.getPayload();
        log.info("Processing coupon applied - orderId: {}, couponId: {}", payload.getOrderId(), payload.getCouponId());

        try {
            couponService.use(payload.getCouponId(), payload.getOrderId());
            log.info("Coupon applied successfully for orderId: {}, couponId: {}", payload.getOrderId(), payload.getCouponId());
        } catch (Exception e) {
            log.error("Failed to coupon applied for orderId: {}, couponId: {}", payload.getOrderId(), payload.getCouponId(), e);
        }
    }

    @KafkaListener(
            topics = EventType.Topic.COUPON_CANCELED,
            groupId = "coupon-canceled-consumer")
    public void consumeCouponCanceled(String message) {
        log.info("[CouponConsumer.consumeCouponCanceled()] Received message: {}", message);
        Event<EventPayload> event = Event.fromJson(message);

        CouponCanceledEventPayload payload = (CouponCanceledEventPayload) event.getPayload();
        log.info("Processing coupon canceled - orderId: {}, couponId: {}", payload.getOrderId(), payload.getCouponId());

        try {
            if (payload.getCouponInfos() != null && !payload.getCouponInfos().isEmpty()) {
                for (CouponCanceledEventPayload.CouponInfo couponInfo : payload.getCouponInfos()) {
                    if (couponInfo.getCouponId() != null) {
                        couponService.cancel(couponInfo.getCouponId());
                        log.info("Coupon canceled successfully for orderId: {}, couponId: {}",
                                payload.getOrderId(), couponInfo.getCouponId());
                    }
                }
            }
            log.info("Coupon applied successfully for orderId: {}, couponId: {}", payload.getOrderId(), payload.getCouponId());
        } catch (Exception e) {
            log.error("Failed to coupon canceled for orderId: {}, couponId: {}", payload.getOrderId(), payload.getCouponId(), e);
        }
    }
}

