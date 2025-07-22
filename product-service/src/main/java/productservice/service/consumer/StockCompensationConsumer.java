package productservice.service.consumer;

import event.Event;
import event.EventPayload;
import event.EventType;
import event.payload.compensation.CompensationCompletedPayload;
import event.payload.compensation.StockCompensationRequestPayload;
import event.publisher.EventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import productservice.service.ProductService;
import productservice.service.dto.ProductOptionRequest;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockCompensationConsumer {

    private final ProductService productService;
    private final EventPublisher eventPublisher;

    @KafkaListener(topics = EventType.Topic.STOCK_COMPENSATION_REQUEST, groupId = "product-stock-compensation-consumer")
    @Transactional
    public void handleStockCompensation(String message) {
        log.info("Received stock compensation request: {}", message);

        try {
            Event<EventPayload> event = Event.fromJson(message);
            StockCompensationRequestPayload payload = (StockCompensationRequestPayload) event.getPayload();

            // 재고 복원 처리
            ProductOptionRequest.StockUpdate stockRestore = ProductOptionRequest.StockUpdate.builder()
                    .productId(payload.getProductId())
                    .optionId(payload.getProductOptionId())
                    .quantity(payload.getQuantity())
                    .build();

            productService.increaseStock(List.of(stockRestore));

            // 성공 이벤트 발행
            publishCompensationCompleted(payload, true, null);

            log.info("Stock compensation completed successfully - sagaId: {}, productId: {}, quantity: {}",
                    payload.getSagaId(), payload.getProductId(), payload.getQuantity());

        } catch (Exception e) {
            log.error("Stock compensation failed: {}", e.getMessage(), e);

            try {
                Event<EventPayload> event = Event.fromJson(message);
                StockCompensationRequestPayload payload = (StockCompensationRequestPayload) event.getPayload();
                publishCompensationCompleted(payload, false, e.getMessage());
            } catch (Exception parseError) {
                log.error("Failed to parse compensation event for error handling: {}", parseError.getMessage());
            }
        }
    }

    private void publishCompensationCompleted(StockCompensationRequestPayload originalPayload, boolean success, String errorMessage) {
        CompensationCompletedPayload completedPayload = CompensationCompletedPayload.builder()
                .sagaId(originalPayload.getSagaId())
                .orderId(originalPayload.getOrderId())
                .userId(originalPayload.getUserId())
                .compensationType("STOCK_RESTORE")
                .success(success)
                .errorMessage(errorMessage)
                .timestamp(System.currentTimeMillis())
                .build();

        eventPublisher.publishEvent(
                EventType.COMPENSATION_COMPLETED,
                completedPayload,
                EventType.Topic.COMPENSATION_COMPLETED
        );
    }
}
