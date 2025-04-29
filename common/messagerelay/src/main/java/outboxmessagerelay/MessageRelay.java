package outboxmessagerelay;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import outboxmessagerelay.entity.Outbox;
import outboxmessagerelay.repository.OutboxRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Getter
@Component
public class MessageRelay {

    private final OutboxRepository outboxRepository;

    private final KafkaTemplate<String, String> kafkaTemplate;

    public MessageRelay(
            OutboxRepository outboxRepository,
            @Qualifier("messageRelayKafkaTemplate") KafkaTemplate<String, String> kafkaTemplate
    ) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void createOutbox(OutboxEvent outboxEvent) {
        log.info("[MessageRelay.createOutbox] outboxEvent={}", outboxEvent);
        outboxRepository.save(outboxEvent.getOutbox());
    }

    /**
     * 트랜잭션이 커밋된 다음 트랜잭션이 커밋되면 이제 비동기로 카프카 이벤트를 전송하는데
     * 여기서 실제 발행을 시행해 줄 건데 아웃박스 이벤트에서 아웃박스만 꺼내 가지고 카프카로 전송
     */
    @Async("messageRelayPublishEventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publishEvent(OutboxEvent outboxEvent) {
        log.info("[MessageRelay.publishEvent] Publishing event");
        publishEvent(outboxEvent.getOutbox());
    }

    private void publishEvent(Outbox outbox) {
        try {
            // Kafka로 메시지 발행
            kafkaTemplate.send(
                    outbox.getTopic(),
                    outbox.getPayload()
            ).get(1, TimeUnit.SECONDS);

            // 메시지 처리 후 바로 삭제
            outboxRepository.delete(outbox);
            log.info("[MessageRelay.publishOutboxMessage] Successfully published message: topic={}, id={}",
                    outbox.getTopic(), outbox.getId());
        } catch (Exception e) {
            log.error("[MessageRelay.publishOutboxMessage] Failed to publish message: outbox={}", outbox, e);
            // 실패 처리는 주기적인 폴링 메서드에서 처리됨
        }
    }

    @Scheduled(
            fixedDelay = 10000,
            initialDelay = 5000
    )
    public void publishPendingMessages() {
        try {
            List<Outbox> pendingMessages = outboxRepository.findAllByCreatedAtLessThanEqualOrderByCreatedAtAsc(
                    LocalDateTime.now().minusSeconds(10),
                    PageRequest.of(0, 100)
            );

            log.debug("[MessageRelay.publishPendingMessages] Found {} pending messages", pendingMessages.size());

            for (Outbox outbox : pendingMessages) {
                publishEvent(outbox);
            }
        } catch (Exception e) {
            log.error("[MessageRelay.publishPendingMessages] Error processing pending messages", e);
        }
    }
}
