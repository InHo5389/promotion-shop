package couponservice.service.v3.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import couponservice.service.v3.CouponService;
import event.Event;
import event.EventPayload;
import event.EventType;
import event.payload.CouponConfirmPayload;
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

@Slf4j
@Component
@RequiredArgsConstructor
public class CouponEventConsumer {

    private final CouponService couponService;
    private final ObjectMapper objectMapper;
    private final OutboxEventPublisher outboxEventPublisher;

    /**
     * 주문 확정 이벤트 처리
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
            topics = EventType.Topic.STOCK_CONFIRM,
            groupId = "coupon-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleStockConfirmed(ConsumerRecord<String, String> record) {
        log.info("===== 쿠폰 확정 이벤트 수신 ===== topic: {}, partition: {}, offset: {}",
                record.topic(), record.partition(), record.offset());

        try {
            Event<EventPayload> event = Event.fromJson(record.value());
            StockConfirmPayload payload = objectMapper.convertValue(
                    event.getPayload(),
                    StockConfirmPayload.class
            );

            couponService.confirmReservation(payload.getOrderId());
            log.info("===== 쿠폰 확정 완료 ===== orderId: {}", payload.getOrderId());

            CouponConfirmPayload nextPayload = CouponConfirmPayload.builder()
                    .orderId(payload.getOrderId())
                    .userId(payload.getUserId())
                    .build();

            outboxEventPublisher.publish(EventType.COUPON_CONFIRM, nextPayload);
        } catch (Exception e) {
            log.error("주문 확정 이벤트 처리 실패 - topic: {}, key: {}", record.topic(), record.key(), e);
            throw e;
        }
    }

    @DltHandler
    @KafkaListener(
            topics = EventType.Topic.STOCK_CONFIRM + "-dlt",
            groupId = "coupon-service-group"
    )
    public void handleStockConfirmedDlt(
            ConsumerRecord<String, String> record,
            @Header(KafkaHeaders.EXCEPTION_MESSAGE) String errorMessage
    ) {
        try {
            Event<EventPayload> event = Event.fromJson(record.value());
            StockConfirmPayload payload = objectMapper.convertValue(
                    event.getPayload(),
                    StockConfirmPayload.class
            );

            log.error(
                    "[DLT] orderId={}, eventType={}, step={}, topic={}, partition={}, offset={}, " +
                            "error={}, previousSteps=[STOCK], payload={}, timestamp={}, service={}",
                    payload.getOrderId(),
                    "STOCK_CONFIRMED",
                    "COUPON",
                    record.topic(),
                    record.partition(),
                    record.offset(),
                    errorMessage,
                    record.value(),
                    System.currentTimeMillis(),
                    "coupon-service"
            );

        } catch (Exception e) {
            log.error("[DLT] 파싱 실패", e);
        }
    }
}