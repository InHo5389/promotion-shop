package timesaleservice.client.serviceclient;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import timesaleservice.client.ProductClient;
import timesaleservice.client.dto.ProductResponse;
import timesaleservice.common.exception.CustomGlobalException;
import timesaleservice.common.exception.ErrorType;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductServiceClient {

    private final ProductClient productClient;

    @CircuitBreaker(name = "productService", fallbackMethod = "getCartFallback")
    public ProductResponse read(Long productId) {
        return productClient.read(productId);
    }

    private ProductResponse readFallback(Long productId, Exception ex) {
        log.error("Failed to get product productId:{}: {}", productId, ex.getMessage());
        throw new CustomGlobalException(ErrorType.PRODUCT_SERVICE_UNAVAILABLE);
    }
}
