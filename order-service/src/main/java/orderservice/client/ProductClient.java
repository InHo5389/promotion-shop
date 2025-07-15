package orderservice.client;

import orderservice.client.dto.ProductOptionRequest;
import orderservice.client.dto.ProductRequest;
import orderservice.client.dto.ProductResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(value = "product-service")
public interface ProductClient {

    @GetMapping("/api/v1/products/{productId}")
    ProductResponse read(@PathVariable Long productId);

    @PostMapping("/api/v1/products/stock/decrease")
    void decreaseStock(@RequestBody List<ProductOptionRequest.StockUpdate> requests);

    @PostMapping("/api/v1/products/stock/increase")
    void increaseStock(@RequestBody List<ProductOptionRequest.StockUpdate> requests);

    @PostMapping("/api/v1/products/batch")
    List<ProductResponse> getProducts(@RequestBody ProductRequest.ReadProductIds request);
}
