package couponservice.service.v3;

import couponservice.common.exception.CustomGlobalException;
import couponservice.common.exception.ErrorType;
import couponservice.common.interceptor.UserIdInterceptor;
import couponservice.entity.Coupon;
import couponservice.entity.CouponPolicy;
import couponservice.outboxmessagerelay.OutboxEventPublisher;
import couponservice.repository.CouponRepository;
import couponservice.repository.v2.CouponLockRepository;
import couponservice.repository.v2.CouponPolicyRedisRepository;
import couponservice.repository.v2.CouponRedisRepository;
import couponservice.service.dto.v1.CouponRequest;
import couponservice.service.dto.v1.CouponResponse;
import couponservice.service.dto.v3.CouponDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 쿠폰 발급 로직을 쿠폰 컨슈머로 위임
 *
 * 프로듀서에서 쿠폰 발급 요청에 대한 메시지를 쓰고
 * 컨슈머에서 해당 메시지를 읽어서 issue() 호출
 *
 * 이렇게 구현할 시 CouponService에서 많은 요청을 받아도 실제 발급하는 요청은 레디스에서
 * 비즈니스 로직을 처리하고 실제 db발급 처리하는 부분은 컨슈머로 위임해서 순차적으로 db에 쌓이는 비동기 구조
 */
@Slf4j
@Service("couponServiceV3")
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;
    private final CouponLockRepository couponLockRepository;
    private final CouponRedisRepository couponRedisRepository;
    private final CouponProducer couponProducer;
    private final CouponPolicyRedisRepository couponPolicyRedisRepository;
    private final OutboxEventPublisher outboxEventPublisher;

    @Transactional(readOnly = true)
    public void requestCouponIssue(CouponRequest.Issue request) {
        RLock lock = couponLockRepository.getLock(request.getCouponPolicyId());

        try {
            if (!couponLockRepository.tryLock(lock)) {
                throw new CustomGlobalException(ErrorType.COUPON_TO_MANY_REQUEST);
            }

            CouponPolicy couponPolicy = couponPolicyRedisRepository.getCouponPolicy(request.getCouponPolicyId())
                    .orElseThrow(() -> new CustomGlobalException(ErrorType.NOT_FOUND_COUPON_POLICY));

            LocalDateTime now = LocalDateTime.now();
            if (now.isBefore(couponPolicy.getStartTime()) || now.isAfter(couponPolicy.getEndTime())) {
                throw new CustomGlobalException(ErrorType.COUPON_NOT_ISSUABLE_PERIOD);
            }

            if (!couponPolicyRedisRepository.decrementQuantity(request.getCouponPolicyId())) {
                throw new CustomGlobalException(ErrorType.COUPON_QUANTITY_EXHAUSTED);
            }

            // Outbox 패턴을 이용한 메시지 발행
            CouponDto.IssueMessage message = CouponDto.IssueMessage.builder()
                    .policyId(request.getCouponPolicyId())
                    .userId(UserIdInterceptor.getCurrentUserId())
                    .build();

            outboxEventPublisher.publishCouponIssueRequest(message);
            log.info("Coupon issue request published to outbox: policyId={}, userId={}",
                    message.getPolicyId(), message.getUserId());
        }finally {
            couponLockRepository.unlock(lock);
        }
    }

    @Transactional
    public void issue(CouponDto.IssueMessage message) {
        try {
            CouponPolicy couponPolicy = couponPolicyRedisRepository.getCouponPolicy(message.getPolicyId())
                    .orElseThrow(() -> new CustomGlobalException(ErrorType.NOT_FOUND_COUPON_POLICY));

            Coupon coupon = Coupon.create(couponPolicy, message.getUserId(), generateCouponCode());
            Coupon savedCoupon = couponRepository.save(coupon);
            log.info("Coupon issued successfully: policyId={}, userId={}", message.getPolicyId(), message.getUserId());

        } catch (Exception e) {
            log.error("Failed to issue coupon: {}", e.getMessage());
            throw e;
        }
    }

    @Transactional
    public CouponResponse.Response use(Long couponId, Long orderId) {
        Coupon coupon = couponRepository.findByIdWithPolicyForUpdate(couponId)
                .orElseThrow(() -> new CustomGlobalException(ErrorType.NOT_FOUND_COUPON));
        coupon.use(orderId);

        couponRedisRepository.updateCouponState(coupon);

        return CouponResponse.Response.from(coupon);
    }

    @Transactional
    public CouponResponse.Response cancel(Long couponId) {
        Coupon coupon = couponRepository.findByIdWithPolicyForUpdate(couponId)
                .orElseThrow(() -> new CustomGlobalException(ErrorType.NOT_FOUND_COUPON));
        coupon.cancel();

        couponRedisRepository.updateCouponState(coupon);

        return CouponResponse.Response.from(coupon);
    }

    private String generateCouponCode() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
