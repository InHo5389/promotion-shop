package orderservice.service.consumer;

import event.Event;
import event.EventPayload;
import event.EventType;
import event.payload.compensation.CompensationCompletedPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import orderservice.entity.SagaStatus;
import orderservice.entity.SagaTransaction;
import orderservice.repository.SagaTransactionRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class CompensationStatusConsumer {

    private final SagaTransactionRepository sagaTransactionRepository;

    @KafkaListener(
            topics = EventType.Topic.COMPENSATION_COMPLETED,
            groupId = "order-compensation-status-consumer"
    )
    @Transactional
    public void handleCompensationCompleted(String message) {
        log.info("Received compensation completed event: {}", message);

        try {
            Event<EventPayload> event = Event.fromJson(message);
            CompensationCompletedPayload payload = (CompensationCompletedPayload) event.getPayload();

            SagaTransaction saga = sagaTransactionRepository.findBySagaId(payload.getSagaId())
                    .orElseThrow(() -> new IllegalArgumentException("SAGA not found: " + payload.getSagaId()));

            if (payload.isSuccess()) {
                log.info("Compensation step completed successfully - sagaId: {}, type: {}",
                        payload.getSagaId(), payload.getCompensationType());

                // 모든 보상이 완료되었는지 확인하는 로직 (간단화)
                checkAndCompleteCompensation(saga, payload);
            } else {
                log.error("Compensation step failed - sagaId: {}, type: {}, error: {}",
                        payload.getSagaId(), payload.getCompensationType(), payload.getErrorMessage());

                saga.updateStatus(SagaStatus.FAILED);
                saga.recordErrorMessage("Compensation failed: " + payload.getErrorMessage());
                sagaTransactionRepository.save(saga);
            }

        } catch (Exception e) {
            log.error("Failed to process compensation completed event: {}", e.getMessage(), e);
        }
    }

    private void checkAndCompleteCompensation(SagaTransaction saga, CompensationCompletedPayload payload) {
        // 실제로는 더 정교한 로직이 필요하지만, 여기서는 단순화
        saga.updateStatus(SagaStatus.COMPENSATION_COMPLETED);
        sagaTransactionRepository.save(saga);

        log.info("All compensation completed for SAGA - sagaId: {}", saga.getSagaId());
    }
}
