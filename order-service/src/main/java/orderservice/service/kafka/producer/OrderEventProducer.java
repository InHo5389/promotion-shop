package orderservice.service.kafka.producer;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import orderservice.service.kafka.event.CouponCancelEvent;
import orderservice.service.kafka.event.OrderConfirmEvent;
import orderservice.service.kafka.event.PointCancelEvent;
import orderservice.service.kafka.event.StockCancelEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    private static final String ORDER_CONFIRM_TOPIC = "order-confirm-topic";
    private static final String STOCK_CANCEL_TOPIC = "stock-cancel-topic";
    private static final String COUPON_CANCEL_TOPIC = "coupon-cancel-topic";
    private static final String POINT_CANCEL_TOPIC = "point-cancel-topic";

    public void sendOrderConfirmEvent(OrderConfirmEvent event) {
        sendEvent(ORDER_CONFIRM_TOPIC, event.getOrderId().toString(), event, "주문 확정");
    }

    public void sendStockCancelEvent(Long orderId) {
        StockCancelEvent event = StockCancelEvent.builder()
                .orderId(orderId)
                .build();
        sendEvent(STOCK_CANCEL_TOPIC, orderId.toString(), event, "재고 취소");
    }

    public void sendCouponCancelEvent(Long orderId) {
        CouponCancelEvent event = CouponCancelEvent.builder()
                .orderId(orderId)
                .build();
        sendEvent(COUPON_CANCEL_TOPIC, orderId.toString(), event, "쿠폰 취소");
    }

    public void sendPointCancelEvent(Long orderId, Long userId) {
        PointCancelEvent event = PointCancelEvent.builder()
                .orderId(orderId)
                .userId(userId)
                .build();
        sendEvent(POINT_CANCEL_TOPIC, orderId.toString(), event, "포인트 취소");
    }

    private void sendEvent(String topic, String key, Object event, String eventType) {
        try {
            // ✅ Object → JSON String 변환
            String jsonPayload = objectMapper.writeValueAsString(event);

            CompletableFuture<SendResult<String, String>> future =
                    kafkaTemplate.send(topic, key, jsonPayload);

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("{} 이벤트 발행 성공 - topic: {}, key: {}, partition: {}, offset: {}",
                            eventType, topic, key,
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                } else {
                    log.error("{} 이벤트 발행 실패 - topic: {}, key: {}",
                            eventType, topic, key, ex);
                }
            });
        } catch (Exception e) {
            log.error("{} 이벤트 발행 중 예외 발생 - topic: {}, key: {}, error: {}",
                    eventType, topic, key, e.getMessage(), e);
        }
    }
}
