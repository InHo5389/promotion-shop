package userservice.service.consumer;

import event.Event;
import event.EventPayload;
import event.EventType;
import event.payload.CartClearedEventPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import userservice.service.CartService;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class CartEventConsumer {

    private final CartService cartService;

    @KafkaListener(
            topics = EventType.Topic.CART,
            groupId = "cart"
    )
    public void consumeCartClear(String message) {
        log.info("[CartEventConsumer] Received message: {}", message);
        Event<EventPayload> event = Event.fromJson(message);

        CartClearedEventPayload payload = (CartClearedEventPayload) event.getPayload();
        log.info("Processing cart clear - userId: {}", payload.getUserId());

        try {
            cartService.clearCart(payload.getUserId());
            log.info("cart cleared successfully for userId: {}", payload.getUserId());
        } catch (Exception e) {
            log.error("Failed to cart clear for userId: {}", payload.getUserId(), e);
        }
    }
}
