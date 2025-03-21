package orderservice.client.serviceclient;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import orderservice.client.ProductClient;
import orderservice.client.dto.ProductOptionRequest;
import orderservice.client.dto.ProductRequest;
import orderservice.client.dto.ProductResponse;
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
        throw new CustomGlobalException(ErrorType.PRODUCT_SERVICE_UNAVAILABLE);
    }

    @CircuitBreaker(name = "productService", fallbackMethod = "decreaseStockFallback")
    public void decreaseStock(List<ProductOptionRequest.StockUpdate> stockUpdates) {
        productClient.decreaseStock(stockUpdates);
    }

    private void decreaseStockFallback(List<ProductOptionRequest.StockUpdate> stockUpdates, Exception ex) {
        log.error("Failed to decrease stock: {}", ex.getMessage());
        throw new CustomGlobalException(ErrorType.PRODUCT_SERVICE_UNAVAILABLE);
    }

    @CircuitBreaker(name = "productService", fallbackMethod = "increaseStockFallback")
    public void increaseStock(List<ProductOptionRequest.StockUpdate> requests) {
        productClient.increaseStock(requests);
    }

    private void increaseStockFallback(List<ProductOptionRequest.StockUpdate> stockUpdates, Exception ex) {
        log.error("Failed to decrease stock: {}", ex.getMessage());
        throw new CustomGlobalException(ErrorType.PRODUCT_SERVICE_UNAVAILABLE);
    }
}