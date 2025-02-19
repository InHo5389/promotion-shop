package couponservice.controller.v2;

import couponservice.entity.CouponStatus;
import couponservice.service.dto.v1.CouponRequest;
import couponservice.service.dto.v1.CouponResponse;
import couponservice.service.v2.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController("couponControllerV2")
@RequestMapping("/api/v2/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    @PostMapping
    public ResponseEntity<CouponResponse.Response> issue(@RequestBody CouponRequest.Issue request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(couponService.issue(request));
    }

    @PostMapping("/{couponId}/use")
    public ResponseEntity<CouponResponse.Response> use(
            @PathVariable Long couponId,
            @RequestBody CouponRequest.Use request) {
        return ResponseEntity.ok(couponService.use(couponId, request.getOrderId()));
    }

    @PostMapping("/{couponId}/cancel")
    public ResponseEntity<CouponResponse.Response> cancel(@PathVariable Long couponId) {
        return ResponseEntity.ok(couponService.cancel(couponId));
    }

    @GetMapping("/{couponId}")
    public ResponseEntity<CouponResponse.Response> getCoupon(@PathVariable Long couponId) {
        return ResponseEntity.ok(couponService.getCoupon(couponId));
    }
}
