package couponservice.service.v3;

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
import couponservice.service.dto.v3.CouponDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import outboxmessagerelay.OutboxEventPublisher;

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

    @Transactional
    public void requestCouponIssue(CouponRequest.Issue request) {
        Long userId = UserIdInterceptor.getCurrentUserId();
        Long policyId = request.getCouponPolicyId();

        log.info("Coupon issue requested - policyId: {}, userId: {}", policyId, userId);

        RLock lock = couponLockRepository.getLock(policyId);

        try {
            if (!couponLockRepository.tryLock(lock)) {
                log.warn("Failed to acquire lock for coupon policy: {} - Too many concurrent requests", policyId);
                throw new CustomGlobalException(ErrorType.COUPON_TO_MANY_REQUEST);
            }
            log.debug("Lock acquired for coupon policy: {}", policyId);

            CouponPolicy couponPolicy = couponPolicyRedisRepository.getCouponPolicy(policyId)
                    .orElseThrow(() -> {
                        log.info("Coupon policy not found: {}", policyId);
                        return new CustomGlobalException(ErrorType.NOT_FOUND_COUPON_POLICY);
                    });

            LocalDateTime now = LocalDateTime.now();
            if (now.isBefore(couponPolicy.getStartTime()) || now.isAfter(couponPolicy.getEndTime())) {
                log.info("Coupon not in issuable period - policyId: {}, startTime: {}, endTime: {}, currentTime: {}",
                        policyId, couponPolicy.getStartTime(), couponPolicy.getEndTime(), now);
                throw new CustomGlobalException(ErrorType.COUPON_NOT_ISSUABLE_PERIOD);
            }

            if (!couponPolicyRedisRepository.decrementQuantity(policyId)) {
                log.info("Coupon quantity exhausted for policy: {}", policyId);
                throw new CustomGlobalException(ErrorType.COUPON_QUANTITY_EXHAUSTED);
            }
            log.debug("Coupon quantity decremented for policy: {}", policyId);

            CouponDto.IssueMessage message = CouponDto.IssueMessage.builder()
                    .policyId(policyId)
                    .userId(userId)
                    .build();

            outboxEventPublisher.publishCouponIssueRequest(message);
            log.info("Coupon issue request published - policyId: {}, userId: {}", policyId, userId);
        }finally {
            couponLockRepository.unlock(lock);
        }
    }

    @Transactional
    public void issue(CouponDto.IssueMessage message) {
        Long policyId = message.getPolicyId();
        Long userId = message.getUserId();

        log.info("Processing coupon issue - policyId: {}, userId: {}", policyId, userId);

        try {
            CouponPolicy couponPolicy = couponPolicyRedisRepository.getCouponPolicy(policyId)
                    .orElseThrow(() -> {
                        log.info("Coupon policy not found during issue process: {}", policyId);
                        return new CustomGlobalException(ErrorType.NOT_FOUND_COUPON_POLICY);
                    });

            String couponCode = generateCouponCode();
            log.debug("Generated coupon code: {} for user: {}", couponCode, userId);

            Coupon coupon = Coupon.create(couponPolicy, userId, couponCode);
            Coupon savedCoupon = couponRepository.save(coupon);

            couponRedisRepository.updateCouponState(savedCoupon);

            log.info("Coupon issued successfully - id: {}, policyId: {}, userId: {}, code: {}",
                    savedCoupon.getId(), policyId, userId, couponCode);
        } catch (Exception e) {
            log.error("Failed to issue coupon - policyId: {}, userId: {}", policyId, userId, e);
            throw e;
        }
    }

    @Transactional
    public CouponResponse.Response use(Long couponId, Long orderId) {
        log.info("Coupon use requested - couponId: {}, orderId: {}", couponId, orderId);

        Coupon coupon = couponRepository.findByIdWithPolicyForUpdate(couponId)
                .orElseThrow(() -> new CustomGlobalException(ErrorType.NOT_FOUND_COUPON));
        log.debug("Coupon found - id: {}, status: {}, userId: {}",
                coupon.getId(), coupon.getStatus(), coupon.getUserId());

        coupon.use(orderId);
        log.info("Coupon used - id: {}, orderId: {}", couponId, orderId);

        couponRedisRepository.updateCouponState(coupon);
        log.debug("Coupon state updated in Redis - id: {}", couponId);

        return CouponResponse.Response.from(coupon);
    }

    @Transactional
    public CouponResponse.Response cancel(Long couponId) {
        log.info("Coupon cancellation requested - couponId: {}", couponId);

        Coupon coupon = couponRepository.findByIdWithPolicyForUpdate(couponId)
                .orElseThrow(() -> new CustomGlobalException(ErrorType.NOT_FOUND_COUPON));
        log.debug("Coupon found for cancellation - id: {}, status: {}, userId: {}",
                coupon.getId(), coupon.getStatus(), coupon.getUserId());

        coupon.cancel();
        log.info("Coupon cancelled - id: {}", couponId);

        // Redis 상태 업데이트
        couponRedisRepository.updateCouponState(coupon);
        log.debug("Coupon state updated in Redis after cancellation - id: {}", couponId);

        return CouponResponse.Response.from(coupon);
    }

    private String generateCouponCode() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
