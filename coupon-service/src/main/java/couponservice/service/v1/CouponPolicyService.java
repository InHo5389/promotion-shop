package couponservice.service.v1;

import couponservice.common.exception.CustomGlobalException;
import couponservice.common.exception.ErrorType;
import couponservice.entity.CouponPolicy;
import couponservice.repository.CouponPolicyRepository;
import couponservice.service.dto.v1.CouponPolicyRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CouponPolicyService {

    private final CouponPolicyRepository couponPolicyRepository;

    @Transactional
    public CouponPolicy create(CouponPolicy couponPolicy) {
        return couponPolicyRepository.save(couponPolicy);
    }

    public CouponPolicy getCouponPolicy(Long id) {
        return couponPolicyRepository.findById(id)
                .orElseThrow(() -> new CustomGlobalException(ErrorType.NOT_FOUND_COUPON_POLICY));
    }

    public List<CouponPolicy> getAllCouponPolicies(){
        return couponPolicyRepository.findAll();
    }
}
