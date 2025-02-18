package couponservice.service.v1;

import couponservice.common.exception.CustomGlobalException;
import couponservice.common.exception.ErrorType;
import couponservice.common.interceptor.UserIdInterceptor;
import couponservice.entity.Coupon;
import couponservice.entity.CouponPolicy;
import couponservice.repository.CouponPolicyRepository;
import couponservice.repository.CouponRepository;
import couponservice.service.dto.v1.CouponRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;
    private final CouponPolicyRepository couponPolicyRepository;

    @Transactional
    public Coupon issue(CouponRequest.Issue request) {

        CouponPolicy couponPolicy = couponPolicyRepository.findByIdWithLock(request.getCouponPolicyId())
                .orElseThrow(() -> new CustomGlobalException(ErrorType.NOT_FOUND_COUPON_POLICY));

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(couponPolicy.getStartTime()) || now.isAfter(couponPolicy.getEndTime())) {
            throw new CustomGlobalException(ErrorType.COUPON_NOT_ISSUABLE_PERIOD);
        }

        Long issuedCouponCount = couponRepository.countByCouponPolicyId(couponPolicy.getId());
        if (issuedCouponCount >= couponPolicy.getTotalQuantity()) {
            throw new CustomGlobalException(ErrorType.COUPON_QUANTITY_EXHAUSTED);
        }

        Coupon coupon = Coupon.create(couponPolicy, UserIdInterceptor.getCurrentUserId(), generateCouponCode());
        return couponRepository.save(coupon);
    }

    @Transactional
    public Coupon use(Long couponId, Long orderId) {
        Long currentUserId = UserIdInterceptor.getCurrentUserId();

        Coupon coupon = couponRepository.findByIdAndUserId(couponId, currentUserId)
                .orElseThrow(() -> new CustomGlobalException(ErrorType.NOT_FOUND_COUPON));

        coupon.use(orderId);
        return coupon;
    }

    @Transactional
    public Coupon cancel(Long couponId) {
        Long currentUserId = UserIdInterceptor.getCurrentUserId();

        Coupon coupon = couponRepository.findByIdAndUserId(couponId, currentUserId)
                .orElseThrow(() -> new CustomGlobalException(ErrorType.NOT_FOUND_COUPON));

        coupon.cancel();
        return coupon;
    }

    @Transactional(readOnly = true)
    public Page<Coupon> getCoupons(CouponRequest.GetList request) {
        Long currentUserId = UserIdInterceptor.getCurrentUserId();

        return couponRepository.findByUserIdAndStatusOrderByCreatedAtDesc(
                currentUserId,
                request.getStatus(),
                PageRequest.of(
                        request.getPage() != null ? request.getPage() : 0,
                        request.getSize() != null ? request.getSize() : 10
                )
        );
    }

    private String generateCouponCode() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
