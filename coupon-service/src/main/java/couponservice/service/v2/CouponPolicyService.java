package couponservice.service.v2;

import com.fasterxml.jackson.core.JsonProcessingException;
import couponservice.common.exception.CustomGlobalException;
import couponservice.common.exception.ErrorType;
import couponservice.entity.CouponPolicy;
import couponservice.repository.CouponPolicyRepository;
import couponservice.repository.v2.CouponPolicyRedisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service("couponPolicyServiceV2")
@RequiredArgsConstructor
public class CouponPolicyService {

    private final CouponPolicyRepository couponPolicyRepository;
    private final CouponPolicyRedisRepository couponPolicyRedisRepository;

    /**
     * db에도 저장이 되고
     * redis 초기 수량 설정이 되고
     */
    @Transactional
    public CouponPolicy create(CouponPolicy couponPolicy){

        CouponPolicy savedCouponPolicy = couponPolicyRepository.save(couponPolicy);
        couponPolicyRedisRepository.initializeQuantity(savedCouponPolicy.getId(), savedCouponPolicy.getTotalQuantity());
        couponPolicyRedisRepository.saveCouponPolicy(savedCouponPolicy);

        return savedCouponPolicy;
    }

    public CouponPolicy getCouponPolicy(Long id) {
        Optional<CouponPolicy> redisResult = couponPolicyRedisRepository.getCouponPolicy(id);

        if (redisResult.isEmpty()) {
            CouponPolicy couponPolicy = couponPolicyRepository.findById(id)
                    .orElseThrow(() -> new CustomGlobalException(ErrorType.NOT_FOUND_COUPON_POLICY));
            couponPolicyRedisRepository.saveCouponPolicy(couponPolicy);

            return couponPolicy;
        }

        return redisResult.get();
    }

    public List<CouponPolicy> getAllCouponPolicies() {
        return couponPolicyRepository.findAll();
    }
}
