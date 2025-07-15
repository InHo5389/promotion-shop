package couponservice.controller.v3;

import couponservice.service.dto.v1.CouponRequest;
import couponservice.service.dto.v1.CouponResponse;
import couponservice.service.v3.CouponService;
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

    @PostMapping("/{couponId}/use/{orderId}")
    public ResponseEntity<CouponResponse.Response> useCoupon(
            @PathVariable Long couponId,
            @PathVariable Long orderId
    ) {
        return ResponseEntity.ok(couponService.use(couponId, orderId));
    }

    @PostMapping("/{couponId}/cancel")
    public ResponseEntity<CouponResponse.Response> cancelCoupon(@PathVariable Long couponId) {
        return ResponseEntity.ok(couponService.cancel(couponId));
    }

    @GetMapping("/{couponId}/{userId}")
    public CouponResponse.Response getCoupon(@PathVariable Long couponId,@PathVariable Long userId){
        return couponService.getCoupon(couponId,userId);
    }
}