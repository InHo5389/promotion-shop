package orderservice.service.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import event.Event;
import event.EventPayload;
import event.EventType;
import event.payload.OrderCompletedPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import orderservice.common.exception.CustomGlobalException;
import orderservice.common.exception.ErrorType;
import orderservice.entity.Order;
import orderservice.repository.OrderJpaRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import outboxmessagerelay.OutboxEventPublisher;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventConsumer {

    private final OrderJpaRepository orderJpaRepository;
    private final ObjectMapper objectMapper;
    private final OutboxEventPublisher outboxEventPublisher;

    /**
     * 주문 완료 이벤트 수신
     * - 포인트 적립까지 모두 완료 후
     */
    @Transactional
    @KafkaListener(
            topics = "point-completed-topic",
            groupId = "order-service-group"
    )
    public void handleOrderCompleted(ConsumerRecord<String, String> record) {
        log.info("[OrderCompletedConsumer.handleOrderCompleted] 주문 완료 이벤트 수신");

        try {
            Event<EventPayload> event = Event.fromJson(record.value());
            OrderCompletedPayload payload = objectMapper.convertValue(
                    event.getPayload(),
                    OrderCompletedPayload.class
            );

            Order order = orderJpaRepository.findById(payload.getOrderId())
                    .orElseThrow(() -> new CustomGlobalException(ErrorType.NOT_FOUND_ORDER));

            order.completed();

            log.info("[OrderCompletedConsumer.handleOrderCompleted] 주문 완료 처리 완료 orderId={}", payload.getOrderId());

            // ORDER_COMPLETED 이벤트 발행 (추후 포인트 적립, 알림 등에서 사용)
            OrderCompletedPayload completedPayload = OrderCompletedPayload.builder()
                    .orderId(payload.getOrderId())
                    .build();

            outboxEventPublisher.publish(EventType.ORDER_COMPLETE, completedPayload);

        } catch (Exception e) {
            log.error("[OrderCompletedConsumer.handleOrderCompleted] 주문 완료 처리 실패", e);
            throw new RuntimeException(e);
        }
    }
}
