package userservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import userservice.client.dto.ProductResponse;

@FeignClient(name = "product-service")
public interface ProductClient {

    @GetMapping("/api/v1/products/{productId}")
    ProductResponse getProduct(@PathVariable String productId);

    @GetMapping("/api/v1/products/options/{optionId}")
    ProductResponse.ProductOptionDto getProductOption(@PathVariable String optionId);
}
