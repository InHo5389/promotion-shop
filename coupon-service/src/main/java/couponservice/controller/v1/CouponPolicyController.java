package couponservice.controller.v1;

import couponservice.service.dto.v1.CouponPolicyRequest;
import couponservice.service.dto.v1.CouponPolicyResponse;
import couponservice.service.dto.v1.CouponRequest;
import couponservice.service.v1.CouponPolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/coupon-policies")
@RequiredArgsConstructor
public class CouponPolicyController {

    private final CouponPolicyService couponPolicyService;

    @PostMapping
    public ResponseEntity<CouponPolicyResponse.Create> create(@RequestBody CouponPolicyRequest.Create request) {
        return ResponseEntity.ok(CouponPolicyResponse.Create.from(couponPolicyService.create(request.toEntity())));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CouponPolicyResponse.Response> getCouponPolicies(@PathVariable Long id) {
        return ResponseEntity.ok(CouponPolicyResponse.Response.from(couponPolicyService.getCouponPolicy(id)));
    }

    @GetMapping
    public ResponseEntity<List<CouponPolicyResponse.Response>> getAllCouponPolicies() {
        return ResponseEntity.ok(couponPolicyService.getAllCouponPolicies().stream()
                .map(CouponPolicyResponse.Response::from)
                .collect(Collectors.toList()));
    }
}
