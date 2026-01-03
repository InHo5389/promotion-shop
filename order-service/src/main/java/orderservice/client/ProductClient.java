package orderservice.client;

import orderservice.client.dto.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(value = "product-service")
public interface ProductClient {

//    @GetMapping("/api/v1/products/{productId}")
//    ProductResponse read(@PathVariable Long productId);

    @PostMapping("/api/v1/products/stock/decrease")
    void decreaseStock(@RequestBody List<ProductOptionRequest.StockUpdate> requests);

    @PostMapping("/api/v1/products/stock/increase")
    void increaseStock(@RequestBody List<ProductOptionRequest.StockUpdate> requests);

    @PostMapping("/api/v1/products/batch")
    List<ProductResponse> getProducts(@RequestBody ProductRequest.ReadProductIds request);

    @PostMapping("/api/v1/products/reserve")
    ResponseEntity<Void> reserveStock(@RequestBody StockReserveRequest request);

    @PostMapping("/api/v1/products/confirm/{orderId}")
    ResponseEntity<Void> confirmStock(@PathVariable Long orderId);

    @PostMapping("/api/v1/products/cancel/{orderId}")
    ResponseEntity<Void> cancelReservation(@PathVariable Long orderId);

    @PostMapping("/api/v1/products/rollback/confirm/{orderId}")
    ResponseEntity<Void> rollbackConfirmStock(@PathVariable Long orderId);

    @PostMapping("/api/v1/products/rollback/reserve/{orderId}")
    ResponseEntity<Void> rollbackReserveStock(@PathVariable Long orderId);

    @GetMapping("/api/v1/products/{id}")
    ProductResponse read(@PathVariable Long id);

    @GetMapping("/api/v1/products/options/{id}")
    ProductOptionDto getProductOption(@PathVariable Long id);
}
