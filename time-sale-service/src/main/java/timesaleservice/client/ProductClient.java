package timesaleservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import timesaleservice.client.dto.ProductResponse;

@FeignClient
public interface ProductClient {

    @GetMapping("/api/v1/products/{id}")
    ProductResponse read(@PathVariable Long id);
}
