package couponservice.service.v2;

import couponservice.common.exception.CustomGlobalException;
import couponservice.common.exception.ErrorType;
import couponservice.common.interceptor.UserIdInterceptor;
import couponservice.entity.Coupon;
import couponservice.entity.CouponPolicy;
import couponservice.repository.CouponRepository;
import couponservice.repository.v2.CouponLockRepository;
import couponservice.repository.v2.CouponPolicyRedisRepository;
import couponservice.repository.v2.CouponRedisRepository;
import couponservice.service.dto.v1.CouponRequest;
import couponservice.service.dto.v1.CouponResponse;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service("couponServiceV2")
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;
    private final CouponRedisRepository couponRedisRepository;
    private final CouponPolicyRedisRepository couponPolicyRedisRepository;
    private final CouponLockRepository couponLockRepository;

    /**
     * 해당 lock key를 가지고 redis에서 제공하는 lock을 만드는 구현체인데
     * redisson에서는 getLock을 가지고 해당 로직에 대하여 lock을 구현할 수 있음
     * lock을 획득한다음 동일한 policy에 대해서 접근이 불가
     * <p>
     * lock이 걸려있으면 대기시간 동안 대기
     */
    @Transactional
    public CouponResponse.Response issue(CouponRequest.Issue request) {
        RLock lock = couponLockRepository.getLock(request.getCouponPolicyId());

        try {
            if (!couponLockRepository.tryLock(lock)) {
                throw new CustomGlobalException(ErrorType.COUPON_TO_MANY_REQUEST);
            }

            if (!couponPolicyRedisRepository.decrementQuantity(request.getCouponPolicyId())) {
                throw new CustomGlobalException(ErrorType.COUPON_QUANTITY_EXHAUSTED);
            }

            CouponPolicy couponPolicy = couponPolicyRedisRepository.getCouponPolicy(request.getCouponPolicyId())
                    .orElseThrow(() -> new CustomGlobalException(ErrorType.NOT_FOUND_COUPON_POLICY));

            LocalDateTime now = LocalDateTime.now();
            if (now.isBefore(couponPolicy.getStartTime()) || now.isAfter(couponPolicy.getEndTime())) {
                throw new CustomGlobalException(ErrorType.COUPON_NOT_ISSUABLE_PERIOD);
            }

            Coupon coupon = Coupon.create(couponPolicy, UserIdInterceptor.getCurrentUserId(), generateCouponCode());
            Coupon savedCoupon = couponRepository.save(coupon);

            couponRedisRepository.updateCouponState(savedCoupon);

            return CouponResponse.Response.from(savedCoupon);
        } finally {
            couponLockRepository.unlock(lock);
        }
    }

//    @Transactional
//    public CouponResponse.Response use(Long couponId, Long orderId) {
//        Coupon coupon = couponRepository.findByIdWithPolicyForUpdate(couponId)
//                .orElseThrow(() -> new CustomGlobalException(ErrorType.NOT_FOUND_COUPON));
//        coupon.use(orderId);
//
//        couponRedisRepository.updateCouponState(coupon);
//
//        return CouponResponse.Response.from(coupon);
//    }

    @Transactional
    public CouponResponse.Response cancel(Long couponId) {
        Coupon coupon = couponRepository.findByIdWithPolicyForUpdate(couponId)
                .orElseThrow(() -> new CustomGlobalException(ErrorType.NOT_FOUND_COUPON));
        coupon.cancel();

        couponRedisRepository.updateCouponState(coupon);

        return CouponResponse.Response.from(coupon);
    }

    public CouponResponse.Response getCoupon(Long couponId) {
        Optional<Coupon> couponFromRedis = couponRedisRepository.getCouponState(couponId);
        if (couponFromRedis.isPresent()) {
            return CouponResponse.Response.from(couponFromRedis.get());
        }

        Coupon couponFromDB =  couponRepository.findByIdWithPolicy(couponId)
                .orElseThrow(() -> new CustomGlobalException(ErrorType.NOT_FOUND_COUPON));
        couponRedisRepository.updateCouponState(couponFromDB);

        return CouponResponse.Response.from(couponFromDB);
    }

    private String generateCouponCode() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}