package orderservice.client;

import orderservice.client.dto.CartResponse;
import orderservice.common.config.FeignClientConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(value = "user-service", configuration = FeignClientConfig.class)
public interface CartClient {
    @GetMapping("/api/v1/carts/{id}")
    CartResponse getCart(@PathVariable Long id);

    @DeleteMapping("/api/v1/carts/{id}/all")
    void clearCart(@PathVariable Long id);
}
