package orderservice.client;

import orderservice.client.dto.CouponReserveRequest;
import orderservice.client.dto.CouponReserveResponse;
import orderservice.client.dto.CouponResponse;
import orderservice.client.dto.CouponValidationResponse;
import orderservice.common.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@FeignClient(
        value = "coupon-service",
        configuration = FeignConfig.class
)
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

    @PostMapping("/api/v3/coupons/reserve")
    ResponseEntity<CouponReserveResponse> reserveCoupon(@RequestBody CouponReserveRequest request);

    @PostMapping("/api/v3/coupons/confirm/{orderId}")
    ResponseEntity<Void> confirmCoupon(@PathVariable Long orderId);

    @PostMapping("/api/v3/coupons/cancel/{orderId}")
    ResponseEntity<Void> cancelReservation(@PathVariable Long orderId);

    @PostMapping("/api/v3/coupons/rollback/confirm/{orderId}")
    ResponseEntity<Void> rollbackConfirmCoupon(@PathVariable Long orderId);

    @PostMapping("/api/v3/coupons/rollback/reserve/{orderId}")
    ResponseEntity<Void> rollbackReserveCoupon(@PathVariable Long orderId);

    @GetMapping("/api/v3/coupons/{couponId}/validate")
    CouponValidationResponse validateCoupon(
            @PathVariable Long couponId,
            @RequestParam Long userId
    );
}
