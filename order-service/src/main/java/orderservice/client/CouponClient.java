package orderservice.client;

import orderservice.client.dto.CouponRequest;
import orderservice.client.dto.CouponResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@FeignClient(value = "coupon-service")
public interface CouponClient {

    @GetMapping("/api/v2/coupons/{couponId}")
    CouponResponse.Response getCoupon(@PathVariable Long couponId);

    @PostMapping("/api/v2/coupons/{couponId}/use")
    CouponResponse.Response use(
            @PathVariable Long couponId,
            @RequestBody CouponRequest.Use request);
}
