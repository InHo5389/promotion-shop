package couponservice.controller.v3;

import com.fasterxml.jackson.core.JsonProcessingException;
import couponservice.service.dto.v1.CouponPolicyRequest;
import couponservice.service.dto.v1.CouponPolicyResponse;
import couponservice.service.v2.CouponPolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController("couponPolicyControllerV3")
@RequestMapping("/api/v3/coupon-policies")
@RequiredArgsConstructor
public class CouponPolicyController {

    private final CouponPolicyService couponPolicyService;

    @PostMapping
    public ResponseEntity<CouponPolicyResponse.Response> createCouponPolicy(@RequestBody CouponPolicyRequest.Create request) throws JsonProcessingException {
        return ResponseEntity.ok()
                .body(CouponPolicyResponse.Response.from(couponPolicyService.create(request.toEntity())));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CouponPolicyResponse.Response> getCouponPolicy(@PathVariable Long id) {
        return ResponseEntity.ok(CouponPolicyResponse.Response.from(couponPolicyService.getCouponPolicy(id)));
    }

    @GetMapping
    public ResponseEntity<List<CouponPolicyResponse.Response>> getAllCouponPolicies() {
        return ResponseEntity.ok(couponPolicyService.getAllCouponPolicies().stream()
                .map(CouponPolicyResponse.Response::from)
                .collect(Collectors.toList()));
    }
}
