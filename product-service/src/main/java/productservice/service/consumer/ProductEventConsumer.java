package productservice.service.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import event.Event;
import event.EventPayload;
import event.EventType;
import event.payload.OrderConfirmPayload;
import event.payload.StockConfirmPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import outboxmessagerelay.OutboxEventPublisher;
import productservice.service.ProductService;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductEventConsumer {

    private final ProductService productService;
    private final ObjectMapper objectMapper;
    private final OutboxEventPublisher outboxEventPublisher;

    /**
     * ===== 1단계: 재고 확정 =====
     * order-confirm-topic 구독
     * 성공 시 → stock-confirmed-topic 발행
     */
    @Transactional
    @RetryableTopic(
            attempts = "4",
            backoff = @Backoff(delay = 1000, multiplier = 2.0),
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
            dltTopicSuffix = "-dlt",
            include = {Exception.class}
    )
    @KafkaListener(
            topics = EventType.Topic.ORDER_CONFIRM,
            groupId = "product-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleOrderConfirm(ConsumerRecord<String, String> record) {
        log.info("===== [ProductEventConsumer] 재고 확정 이벤트 수신 ===== topic: {}, partition: {}, offset: {}",
                record.topic(), record.partition(), record.offset());

        try {
            Event<EventPayload> event = Event.fromJson(record.value());
            OrderConfirmPayload orderConfirmPayload = objectMapper.convertValue(
                    event.getPayload(),
                    OrderConfirmPayload.class
            );
            productService.confirmReservation(orderConfirmPayload.getOrderId());
            log.info("===== 재고 확정 완료 ===== orderId: {}", orderConfirmPayload.getOrderId());

            StockConfirmPayload stockConfirmPayload = StockConfirmPayload.builder()
                    .orderId(orderConfirmPayload.getOrderId())
                    .userId(orderConfirmPayload.getUserId())
                    .build();

            outboxEventPublisher.publish(EventType.STOCK_CONFIRM, stockConfirmPayload);
        } catch (Exception e) {
            log.error("주문 확정 이벤트 처리 실패", e);
            throw e;
        }
    }

    /**
     * 🔥 order-confirm-topic DLT 처리
     * - 구조화된 로그만 남김 (Kibana가 수집)
     * - Kibana Alert이 자동으로 알림 발송
     */
    @DltHandler
    @KafkaListener(
            topics = EventType.Topic.ORDER_CONFIRM + "-dlt",
            groupId = "product-service-group"
    )
    public void handleOrderConfirmDlt(
            ConsumerRecord<String, String> record,
            @Header(KafkaHeaders.EXCEPTION_MESSAGE) String errorMessage
    ) {
        try {
            Event<EventPayload> event = Event.fromJson(record.value());
            OrderConfirmPayload payload = objectMapper.convertValue(
                    event.getPayload(),
                    OrderConfirmPayload.class
            );

            // Kibana가 수집할 로그
            log.error(
                    "[DLT] orderId={}, eventType={}, step={}, topic={}, partition={}, offset={}, " +
                            "error={}, previousSteps=[], payload={}, timestamp={}, service={}",
                    payload.getOrderId(),
                    "ORDER_CONFIRM",
                    "STOCK",
                    record.topic(),
                    record.partition(),
                    record.offset(),
                    errorMessage,
                    record.value(),
                    System.currentTimeMillis(),
                    "product-service"
            );

        } catch (Exception e) {
            log.error("[DLT] 파싱 실패 - key={}, value={}, error={}",
                    record.key(), record.value(), e.getMessage(), e);
        }
    }
}
