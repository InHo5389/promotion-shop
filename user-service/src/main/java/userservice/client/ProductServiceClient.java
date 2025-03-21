package userservice.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;
import userservice.client.dto.ProductResponse;
import userservice.common.exception.CustomGlobalException;
import userservice.common.exception.ErrorType;
import userservice.service.domain.CartItem;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceClient {

    private final ProductClient productClient;

    @CircuitBreaker(name = "productService", fallbackMethod = "addToCartFallback")
    public ProductResponse getProduct(String productId) {
        return productClient.getProduct(productId);
    }

    private ProductResponse addToCartFallback(Exception ex) {
        log.error("상품 정보 조회 실패: {}", ex.getMessage());
        throw new CustomGlobalException(ErrorType.PRODUCT_SERVICE_UNAVAILABLE);
    }
}
