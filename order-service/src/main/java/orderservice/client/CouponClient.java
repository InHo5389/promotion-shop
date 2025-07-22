package orderservice.client;

import orderservice.client.dto.CouponResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(value = "coupon-service")
public interface CouponClient {

    @GetMapping("/api/v2/coupons/{couponId}")
    CouponResponse.Response getCoupon(@PathVariable Long couponId);

    @PostMapping("/api/v3/coupons/{couponId}/use/{orderId}")
    ResponseEntity<CouponResponse.Response> useCoupon(
            @PathVariable Long couponId,
            @PathVariable Long orderId
    );

    @GetMapping("/api/v3/coupons/{couponId}/{userId}")
    CouponResponse.Response getCoupon(@PathVariable Long couponId,@PathVariable Long userId);

    @PostMapping("/api/v3/coupons/{couponId}/cancel")
    ResponseEntity<CouponResponse.Response> cancelCoupon(@PathVariable Long couponId);
}
