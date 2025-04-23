package productservice.service.consumer;

import event.Event;
import event.EventPayload;
import event.EventType;
import event.payload.StockDecreasedEventPayload;
import event.payload.StockIncreasedEventPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import productservice.facade.RedissonLockDecreaseStockFacade;
import productservice.facade.RedissonLockIncreaseFacade;
import productservice.service.dto.ProductOptionRequest;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductStockEventConsumer {

    private final RedissonLockDecreaseStockFacade decreaseStockFacade;
    private final RedissonLockIncreaseFacade increaseStockFacade;

    @KafkaListener(
            topics = EventType.Topic.PRODUCT_STOCK_DECREASE,
            groupId = "product-stock-consumer"
    )
    public void consumeProductStockDecrease(String message) {
        log.info("[ProductStockEventConsumer] Received message: {}", message);

        Event<EventPayload> event = Event.fromJson(message);

        StockDecreasedEventPayload payload = (StockDecreasedEventPayload) event.getPayload();
        log.info("Processing stock decrease - orderId: {}", payload.getOrderId());

        try {
            List<ProductOptionRequest.StockUpdate> stockUpdates = payload.getItems().stream()
                    .map(item -> {
                        return ProductOptionRequest.StockUpdate.builder()
                                .productId(item.getProductId())
                                .optionId(item.getProductOptionId())
                                .quantity(item.getQuantity())
                                .build();
                    })
                    .collect(Collectors.toList());

            decreaseStockFacade.decreaseStock(stockUpdates);
            log.info("Stock decreased successfully for orderId: {}", payload.getOrderId());
        } catch (Exception e) {
            log.error("Failed to decrease stock for orderId: {}", payload.getOrderId(), e);
        }
    }

    @KafkaListener(
            topics = EventType.Topic.PRODUCT_STOCK_INCREASE,
            groupId = "product-stock-consumer"
    )
    public void consumeProductStockIncrease(String message) {
        log.info("[ProductStockEventConsumer] Received message: {}", message);

        Event<EventPayload> event = Event.fromJson(message);

        StockIncreasedEventPayload payload = (StockIncreasedEventPayload) event.getPayload();
        log.info("Processing stock increase - orderId: {}", payload.getOrderId());

        try {
            List<ProductOptionRequest.StockUpdate> stockUpdates = payload.getItems().stream()
                    .map(item -> {
                        return ProductOptionRequest.StockUpdate.builder()
                                .productId(item.getProductId())
                                .optionId(item.getProductOptionId())
                                .quantity(item.getQuantity())
                                .build();
                    })
                    .collect(Collectors.toList());

            increaseStockFacade.increaseStock(stockUpdates);
            log.info("Stock increase successfully for orderId: {}", payload.getOrderId());
        } catch (Exception e) {
            log.error("Failed to increase stock for orderId: {}", payload.getOrderId(), e);
        }
    }
}
