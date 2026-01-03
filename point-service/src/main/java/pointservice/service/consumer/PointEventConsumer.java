package pointservice.service.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import event.Event;
import event.EventPayload;
import event.EventType;
import event.payload.CouponConfirmPayload;
import event.payload.PointConfirmPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import outboxmessagerelay.OutboxEventPublisher;
import pointservice.service.v2.PointService;

@Slf4j
@Component
@RequiredArgsConstructor
public class PointEventConsumer {

    private final PointService pointService;
    private final ObjectMapper objectMapper;
    private final OutboxEventPublisher outboxEventPublisher;

    @Transactional
    @RetryableTopic(
            attempts = "4",
            backoff = @Backoff(delay = 1000, multiplier = 2.0),
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
            dltTopicSuffix = "-dlt"
    )
    @KafkaListener(
            topics = EventType.Topic.COUPON_CONFIRM,
            groupId = "point-service-group"
            , containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleCouponConfirmed(ConsumerRecord<String, String> record) {
        log.info("===== [3단계] 포인트 사용 이벤트 수신 =====");

        try {
            Event<EventPayload> event = Event.fromJson(record.value());
            CouponConfirmPayload payload = objectMapper.convertValue(
                    event.getPayload(),
                    CouponConfirmPayload.class
            );

            pointService.confirmReservation(payload.getOrderId(), payload.getUserId());

            log.info("===== [Consumer] 포인트 확정 처리 완료 ===== orderId: {}", payload.getOrderId());

            PointConfirmPayload nextPayload = PointConfirmPayload.builder()
                    .orderId(payload.getOrderId())
                    .build();

            outboxEventPublisher.publish(EventType.POINT_CONFIRM, nextPayload);
        } catch (Exception e) {
            log.error("주문 확정 이벤트 처리 실패", e);
            throw e;
        }
    }

    @DltHandler
    @KafkaListener(
            topics = EventType.Topic.COUPON_CONFIRM + "-dlt",
            groupId = "point-service-group"
    )
    public void handleCouponConfirmedDlt(ConsumerRecord<String, String> record) {
        try {
            CouponConfirmPayload event = objectMapper.readValue(
                    record.value(), CouponConfirmPayload.class);

            log.error(
                    "[DLT] orderId={}, eventType={}, topic={}, partition={}, offset={}, " +
                            "payload={}, timestamp={}, service={}",
                    event.getOrderId(),
                    "COUPON_CONFIRM",
                    record.topic(),
                    record.partition(),
                    record.offset(),
                    record.value(),
                    System.currentTimeMillis(),
                    "point-service"
            );

        } catch (Exception e) {
            log.error("[DLT] 파싱 실패 - key={}, value={}, error={}",
                    record.key(), record.value(), e.getMessage(), e);
        }
    }


}