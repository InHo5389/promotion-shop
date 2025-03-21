package orderservice.client.serviceclient;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import orderservice.client.CartClient;
import orderservice.client.dto.CartResponse;
import orderservice.common.exception.CustomGlobalException;
import orderservice.common.exception.ErrorType;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartServiceClient {

    private final CartClient cartClient;

    @CircuitBreaker(name = "cartService", fallbackMethod = "getCartFallback")
    public CartResponse getCart(Long userId) {
        return cartClient.getCart(userId);
    }

    private CartResponse getCartFallback(Long userId, Exception ex) {
        log.error("Failed to get cart for user {}: {}", userId, ex.getMessage());
        throw new CustomGlobalException(ErrorType.CART_SERVICE_UNAVAILABLE);
    }

    @CircuitBreaker(name = "cartService", fallbackMethod = "clearCartFallback")
    public void clearCart(Long userId) {
        cartClient.clearCart(userId);
    }

    private void clearCartFallback(Long userId, Exception ex) {
        log.error("Failed to clear cart for user {}: {}", userId, ex.getMessage());
        log.warn("Order created but cart was not cleared for user: {}", userId);
    }
}
