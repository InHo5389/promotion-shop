package orderservice.client.serviceclient;

import feign.FeignException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import orderservice.client.ProductClient;
import orderservice.client.dto.*;
import orderservice.common.exception.CustomGlobalException;
import orderservice.common.exception.ErrorType;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceClient {

    private final ProductClient productClient;

    @CircuitBreaker(name = "productService", fallbackMethod = "getProductsFallback")
    public List<ProductResponse> getProducts(List<Long> productIds) {
        return productClient.getProducts(new ProductRequest.ReadProductIds(productIds));
    }

    private List<ProductResponse> getProductsFallback(List<Long> productIds, Exception ex) {
        log.error("Failed to get products: {}", ex.getMessage());
        if (ex instanceof FeignException) {
            throw (FeignException) ex;
        }
        throw new CustomGlobalException(ErrorType.PRODUCT_SERVICE_UNAVAILABLE);
    }

    @Retry(name = "productService")
    @CircuitBreaker(name = "productService", fallbackMethod = "reserveStockFallback")
    public void reserveStock(StockReserveRequest request) {
        productClient.reserveStock(request);
    }

    private void reserveStockFallback(StockReserveRequest request, Exception ex) {
        log.error("Failed to reserve stock: {}", ex.getMessage());
        if (ex instanceof FeignException) {
            throw (FeignException) ex;
        }
        throw new CustomGlobalException(ErrorType.PRODUCT_SERVICE_UNAVAILABLE);
    }

    @Retry(name = "productService")
    @CircuitBreaker(name = "productService", fallbackMethod = "confirmStockFallback")
    public void confirmStock(Long orderId) {
        productClient.confirmStock(orderId);
    }

    private void confirmStockFallback(Long orderId, Exception ex) {
        log.error("재고 확정 실패 fallback. orderId: {}, error: {}", orderId, ex.getMessage());
        if (ex instanceof FeignException) {
            throw (FeignException) ex;
        }
        throw new CustomGlobalException(ErrorType.PRODUCT_SERVICE_UNAVAILABLE);
    }

    @Retry(name = "productService")
    @CircuitBreaker(name = "productService", fallbackMethod = "cancelReservationFallback")
    public void cancelReservation(Long orderId) {
        productClient.cancelReservation(orderId);
    }

    private void cancelReservationFallback(Long orderId, Exception ex) {
        log.error("재고 취소 실패 fallback. orderId: {}, error: {}", orderId, ex.getMessage());
        log.warn("재고 취소 실패했지만 계속 진행. orderId: {}", orderId);
    }

    @Retry(name = "productService")
    @CircuitBreaker(name = "productService", fallbackMethod = "rollbackConfirmStockFallback")
    public void rollbackConfirmStock(Long orderId) {
        log.info("재고 확정 롤백 요청. orderId: {}", orderId);
        productClient.rollbackConfirmStock(orderId);
        log.info("재고 확정 롤백 성공. orderId: {}", orderId);
    }

    private void rollbackConfirmStockFallback(Long orderId, Exception ex) {
        log.error("재고 확정 롤백 실패. orderId: {}, error: {}", orderId, ex.getMessage());
        log.warn("재고 확정 롤백 실패했지만 계속 진행. orderId: {}", orderId);
    }

    @Retry(name = "productService")
    @CircuitBreaker(name = "productService", fallbackMethod = "rollbackReserveStockFallback")
    public void rollbackReserveStock(Long orderId) {
        log.info("재고 확정 롤백 요청. orderId: {}", orderId);
        productClient.rollbackReserveStock(orderId);
        log.info("재고 확정 롤백 성공. orderId: {}", orderId);
    }

    private void rollbackReserveStockFallback(Long orderId, Exception ex) {
        log.error("재고 확정 롤백 실패. orderId: {}, error: {}", orderId, ex.getMessage());
        log.warn("재고 확정 롤백 실패했지만 계속 진행. orderId: {}", orderId);
    }

    @CircuitBreaker(name = "productService", fallbackMethod = "getProductFallback")
    public ProductResponse read(Long productId) {
        return productClient.read(productId);
    }

    private ProductResponse getProductFallback(Long productId, Exception ex) {
        log.error("Failed to get product: {}", ex.getMessage());
        if (ex instanceof FeignException) {
            throw (FeignException) ex;
        }
        throw new CustomGlobalException(ErrorType.PRODUCT_SERVICE_UNAVAILABLE);
    }

    @CircuitBreaker(name = "productService", fallbackMethod = "decreaseStockFallback")
    public void decreaseStock(List<ProductOptionRequest.StockUpdate> stockUpdates) {
        productClient.decreaseStock(stockUpdates);
    }

    private void decreaseStockFallback(List<ProductOptionRequest.StockUpdate> stockUpdates, Exception ex) {
        log.error("Failed to decrease stock: {}", ex.getMessage());
        if (ex instanceof FeignException) {
            throw (FeignException) ex;
        }
        throw new CustomGlobalException(ErrorType.PRODUCT_SERVICE_UNAVAILABLE);
    }

    @CircuitBreaker(name = "productService", fallbackMethod = "increaseStockFallback")
    public void increaseStock(List<ProductOptionRequest.StockUpdate> requests) {
        productClient.increaseStock(requests);
    }

    private void increaseStockFallback(List<ProductOptionRequest.StockUpdate> stockUpdates, Exception ex) {
        log.error("Failed to decrease stock: {}", ex.getMessage());
        if (ex instanceof FeignException) {
            throw (FeignException) ex;
        }
        throw new CustomGlobalException(ErrorType.PRODUCT_SERVICE_UNAVAILABLE);
    }

    @CircuitBreaker(name = "productService", fallbackMethod = "getProductOptionFallback")
    public ProductOptionDto getProductOption(Long productOptionId) {
        return productClient.getProductOption(productOptionId);
    }

    private ProductOptionDto getProductOptionFallback(Long productOptionId, Exception ex) {
        log.error("Failed to decrease stock: {}", ex.getMessage());
        if (ex instanceof FeignException) {
            throw (FeignException) ex;
        }
        throw new CustomGlobalException(ErrorType.PRODUCT_SERVICE_UNAVAILABLE);
    }
}