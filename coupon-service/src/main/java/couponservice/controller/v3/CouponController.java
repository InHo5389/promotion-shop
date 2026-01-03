package couponservice.controller.v3;

import couponservice.service.dto.v1.CouponRequest;
import couponservice.service.dto.v1.CouponResponse;
import couponservice.service.v3.CouponService;
import couponservice.service.v3.dto.CouponReserveRequest;
import couponservice.service.v3.dto.CouponReserveResponse;
import couponservice.service.v3.dto.CouponValidationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController("couponControllerV3")
@RequiredArgsConstructor
@RequestMapping("/api/v3/coupons")
public class CouponController {
    private final CouponService couponService;

    @PostMapping
    public ResponseEntity<Void> issueCoupon(@RequestBody CouponRequest.Issue request) {
        couponService.requestCouponIssue(request);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/reserve")
    public ResponseEntity<CouponReserveResponse> reserveCoupon(@RequestBody CouponReserveRequest request) {
        return ResponseEntity.ok(couponService.reserveCoupons(request));
    }

    @PostMapping("/confirm/{orderId}")
    public ResponseEntity<Void> confirm(@PathVariable Long orderId) {
        couponService.confirmReservation(orderId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/cancel/{orderId}")
    public ResponseEntity<Void> cancelReservation(@PathVariable Long orderId) {
        couponService.cancelReservation(orderId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/rollback/reserve/{orderId}")
    public ResponseEntity<Void> rollbackReserveCoupon(@PathVariable Long orderId) {
        couponService.rollbackReservation(orderId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/rollback/confirm/{orderId}")
    public ResponseEntity<Void> rollbackConfirmCoupon(@PathVariable Long orderId) {
        couponService.rollbackConfirmation(orderId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{couponId}/cancel")
    public ResponseEntity<CouponResponse.Response> cancelCoupon(@PathVariable Long couponId) {
        return ResponseEntity.ok(couponService.cancel(couponId));
    }

    @GetMapping("/{couponId}/{userId}")
    public CouponResponse.Response getCoupon(@PathVariable Long couponId, @PathVariable Long userId) {
        return couponService.getCoupon(couponId, userId);
    }

    @GetMapping("/{couponId}/validate")
    public ResponseEntity<CouponValidationResponse> validateCoupon(
            @PathVariable Long couponId,
            @RequestParam Long userId
    ) {
        return ResponseEntity.ok(couponService.validateCoupon(couponId, userId));
    }
}