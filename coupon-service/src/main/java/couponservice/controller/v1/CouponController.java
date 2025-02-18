package couponservice.controller.v1;

import couponservice.entity.CouponStatus;
import couponservice.service.dto.v1.CouponRequest;
import couponservice.service.dto.v1.CouponResponse;
import couponservice.service.v1.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    @PostMapping
    public ResponseEntity<CouponResponse.Response> issue(@RequestBody CouponRequest.Issue request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CouponResponse.Response.from(couponService.issue(request)));
    }

    @PostMapping("/{couponId}/use")
    public ResponseEntity<CouponResponse.Response> use(
            @PathVariable Long couponId,
            @RequestBody CouponRequest.Use request) {
        return ResponseEntity.ok(CouponResponse.Response.from(couponService.use(couponId, request.getOrderId())));
    }

    @PostMapping("/{couponId}/cancel")
    public ResponseEntity<CouponResponse.Response> cancel(@PathVariable Long couponId) {
        return ResponseEntity.ok(CouponResponse.Response.from(couponService.cancel(couponId)));
    }

    @GetMapping
    public ResponseEntity<List<CouponResponse.Response>> getCoupons(
            @RequestParam(required = false) CouponStatus status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {

        CouponRequest.GetList request = CouponRequest.GetList.builder()
                .status(status)
                .page(page)
                .size(size)
                .build();
        return ResponseEntity.ok(couponService.getCoupons(request).stream()
                .map(CouponResponse.Response::from)
                .toList());
    }
}
