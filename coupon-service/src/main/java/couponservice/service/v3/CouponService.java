package couponservice.service.v3;

import couponservice.common.exception.CustomGlobalException;
import couponservice.common.exception.ErrorType;
import couponservice.common.interceptor.UserIdInterceptor;
import couponservice.entity.*;
import couponservice.repository.CouponRepository;
import couponservice.repository.CouponTransactionHistoryJpaRepository;
import couponservice.repository.v2.CouponLockRepository;
import couponservice.repository.v2.CouponPolicyRedisRepository;
import couponservice.repository.v2.CouponRedisRepository;
import couponservice.service.dto.v1.CouponRequest;
import couponservice.service.dto.v1.CouponResponse;
import couponservice.service.dto.v3.CouponDto;
import couponservice.service.v3.dto.CouponReserveRequest;
import couponservice.service.v3.dto.CouponReserveResponse;
import couponservice.service.v3.dto.CouponValidationResponse;
import couponservice.service.v3.dto.ItemDiscountDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import outboxmessagerelay.OutboxEventPublisher;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static couponservice.entity.CouponTransactionHistory.*;

/**
 * 쿠폰 발급 로직을 쿠폰 컨슈머로 위임
 * <p>
 * 프로듀서에서 쿠폰 발급 요청에 대한 메시지를 쓰고
 * 컨슈머에서 해당 메시지를 읽어서 issue() 호출
 * <p>
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
    private final CouponTransactionHistoryJpaRepository couponTransactionHistoryJpaRepository;

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
        } finally {
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
    public CouponReserveResponse reserveCoupons(CouponReserveRequest request) {
        Long orderId = request.orderId();
        Long userId = request.userId();
        List<CouponReserveRequest.CouponItem> couponItems = request.couponItems();

        log.info("===== 쿠폰 예약 시작 ===== orderId: {}, userId: {}", orderId, userId);

        // 멱등성 체크: 이미 예약된 주문인지 확인
        List<CouponTransactionHistory> existingHistories =
                couponTransactionHistoryJpaRepository.findByOrderIdAndType(orderId, TransactionType.RESERVE);

        Integer totalDiscount = 0;
        List<ItemDiscountDto> itemDiscounts = new ArrayList<>();

        if (!existingHistories.isEmpty()) {
            log.warn("이미 쿠폰이 예약된 주문 - orderId: {}", orderId);

            itemDiscounts = existingHistories.stream()
                    .map(couponTransactionHistory -> new ItemDiscountDto(
                            couponTransactionHistory.getProductOptionId(),
                            couponTransactionHistory.getCouponId(),
                            couponTransactionHistory.getDiscountAmount()
                    ))
                    .toList();
            return new CouponReserveResponse(totalDiscount, itemDiscounts);
        }

        for (CouponReserveRequest.CouponItem couponItem : couponItems) {
            Coupon coupon = couponRepository.findById(couponItem.couponId())
                    .orElseThrow(() -> new CustomGlobalException(ErrorType.COUPON_NOT_FOUND));

            if (!coupon.getUserId().equals(userId)) {
                throw new CustomGlobalException(ErrorType.COUPON_NOT_OWNED);
            }

            coupon.reserve(orderId);

            Integer discountAmount = calculateDiscount(
                    coupon.getCouponPolicy(),
                    couponItem.productPrice()
            );
            totalDiscount += discountAmount;

            ItemDiscountDto itemDiscount = ItemDiscountDto.builder()
                    .productOptionId(couponItem.productOptionId())
                    .couponId(coupon.getId())
                    .discountAmount(discountAmount)
                    .build();
            itemDiscounts.add(itemDiscount);

            CouponTransactionHistory history = create(
                    orderId,
                    coupon.getId(),
                    userId,
                    couponItem.productOptionId(),
                    discountAmount,
                    TransactionType.RESERVE
            );
            couponTransactionHistoryJpaRepository.save(history);

            log.info("쿠폰 예약 완료 - orderId: {}, couponId: {}, discount: {}",
                    orderId, coupon.getId(), discountAmount);
        }

        return CouponReserveResponse.builder()
                .totalDiscount(totalDiscount)
                .itemDiscounts(itemDiscounts)
                .build();
    }

    @Transactional
    public void confirmReservation(Long orderId) {
        log.info("===== 쿠폰 확정 시작 ===== orderId: {}", orderId);

        // 멱등성 체크: 이미 확정된 주문인지 확인
        List<CouponTransactionHistory> confirmHistories =
                couponTransactionHistoryJpaRepository.findByOrderIdAndType(orderId, TransactionType.CONFIRM_RESERVE);

        if (!confirmHistories.isEmpty()) {
            log.warn("이미 쿠폰이 확정된 주문 - orderId: {}", orderId);
            return;
        }

        List<CouponTransactionHistory> reserveHistories =
                couponTransactionHistoryJpaRepository.findByOrderIdAndType(orderId, TransactionType.RESERVE);

        if (reserveHistories.isEmpty()) {
            log.warn("쿠폰 예약 히스토리 없음 - orderId: {}", orderId);
            return;
        }

        for (CouponTransactionHistory history : reserveHistories) {
            Coupon coupon = couponRepository.findById(history.getCouponId())
                    .orElseThrow(() -> new CustomGlobalException(ErrorType.COUPON_NOT_FOUND));

            coupon.confirmReservation();

            CouponTransactionHistory confirmHistory = create(
                    orderId,
                    history.getCouponId(),
                    history.getUserId(),
                    history.getProductOptionId(),
                    history.getDiscountAmount(),
                    TransactionType.CONFIRM_RESERVE
            );
            couponTransactionHistoryJpaRepository.save(confirmHistory);

            log.info("쿠폰 확정 완료 - orderId: {}, couponId: {}", orderId, history.getCouponId());
        }
    }

    @Transactional
    public void cancelReservation(Long orderId) {
        log.info("===== 쿠폰 예약 취소 시작 ===== orderId: {}", orderId);

        // 멱등성 체크
        List<CouponTransactionHistory> cancelHistories =
                couponTransactionHistoryJpaRepository.findByOrderIdAndType(orderId, TransactionType.CANCEL_RESERVE);

        if (!cancelHistories.isEmpty()) {
            log.warn("이미 쿠폰 예약이 취소된 주문 - orderId: {}", orderId);
            return;
        }

        List<CouponTransactionHistory> reserveHistories =
                couponTransactionHistoryJpaRepository.findByOrderIdAndType(orderId, TransactionType.RESERVE);

        if (reserveHistories.isEmpty()) {
            log.warn("쿠폰 예약 히스토리 없음 - orderId: {}", orderId);
            return;
        }

        for (CouponTransactionHistory history : reserveHistories) {
            Coupon coupon = couponRepository.findById(history.getCouponId())
                    .orElseThrow(() -> new CustomGlobalException(ErrorType.COUPON_NOT_FOUND));

            coupon.cancelReservation();

            CouponTransactionHistory cancelHistory = create(
                    orderId,
                    history.getCouponId(),
                    history.getUserId(),
                    history.getProductOptionId(),
                    history.getDiscountAmount(),
                    TransactionType.CANCEL_RESERVE
            );
            couponTransactionHistoryJpaRepository.save(cancelHistory);

            log.info("쿠폰 취소 완료 - orderId: {}, couponId: {}", orderId, history.getCouponId());
        }
    }

    @Transactional
    public void saveFailedEvent(Long orderId, String eventType, String payload, String errorMessage) {
        log.info("실패 이벤트 로그 저장 - orderId: {}, eventType: {}", orderId, eventType);

//        FailedEventLog failedEvent = FailedEventLog.builder()
//                .orderId(orderId)
//                .eventType(eventType)
//                .payload(payload)
//                .errorMessage(errorMessage)
//                .failedAt(LocalDateTime.now())
//                .retryCount(0)
//                .processed(false)
//                .build();
//
//        failedEventLogRepository.save(failedEvent);
//
//        log.info("실패 이벤트 로그 저장 완료 - id: {}", failedEvent.getId());
    }

    @Transactional
    public void rollbackConfirmation(Long orderId) {
        log.info("===== 쿠폰 확정 롤백 시작 ===== orderId: {}", orderId);

        // 멱등성 체크: 이미 롤백된 주문인지 확인
        List<CouponTransactionHistory> rollbackHistories =
                couponTransactionHistoryJpaRepository.findByOrderIdAndType(orderId, TransactionType.ROLLBACK_CONFIRM);

        if (!rollbackHistories.isEmpty()) {
            log.warn("이미 쿠폰 확정이 롤백된 주문 - orderId: {}", orderId);
            return;
        }

        List<CouponTransactionHistory> confirmHistories =
                couponTransactionHistoryJpaRepository.findByOrderIdAndType(orderId, TransactionType.CONFIRM_RESERVE);

        if (confirmHistories.isEmpty()) {
            log.warn("쿠폰 확정 히스토리 없음 - orderId: {}", orderId);
            throw new CustomGlobalException(ErrorType.COUPON_CONFIRMATION_NOT_FOUND);
        }

        for (CouponTransactionHistory history : confirmHistories) {
            Coupon coupon = couponRepository.findById(history.getCouponId())
                    .orElseThrow(() -> new RuntimeException("쿠폰을 찾을 수 없습니다."));

            coupon.rollbackConfirmation();

            CouponTransactionHistory cancelHistory = create(
                    orderId,
                    history.getCouponId(),
                    history.getUserId(),
                    history.getProductOptionId(),
                    history.getDiscountAmount(),
                    TransactionType.ROLLBACK_CONFIRM
            );
            couponTransactionHistoryJpaRepository.save(cancelHistory);

        }

        log.info("쿠폰 확정 롤백 완료. orderId: {}", orderId);
    }

    @Transactional
    public void rollbackReservation(Long orderId) {
        log.info("===== 쿠폰 재예약 시작 (보상) ===== orderId: {}", orderId);

        // 멱등성 체크
        List<CouponTransactionHistory> rollbackHistories =
                couponTransactionHistoryJpaRepository.findByOrderIdAndType(orderId, TransactionType.ROLLBACK_RESERVE);

        if (!rollbackHistories.isEmpty()) {
            log.warn("이미 쿠폰이 재예약된 주문 - orderId: {}", orderId);
            return;
        }

        // 취소된 내역 조회
        List<CouponTransactionHistory> cancelHistories =
                couponTransactionHistoryJpaRepository.findByOrderIdAndType(orderId, TransactionType.CANCEL_RESERVE);

        if (cancelHistories.isEmpty()) {
            log.info("쿠폰 취소 히스토리 없음 - orderId: {}", orderId);
            return;
        }

        for (CouponTransactionHistory history : cancelHistories) {
            Coupon coupon = couponRepository.findById(history.getCouponId())
                    .orElseThrow(() -> new CustomGlobalException(ErrorType.NOT_FOUND_COUPON));

            // 재예약
            coupon.reserve(orderId);

            // 롤백 이력 저장
            CouponTransactionHistory rollbackHistory = CouponTransactionHistory.create(
                    orderId,
                    history.getCouponId(),
                    history.getUserId(),
                    history.getProductOptionId(),
                    history.getDiscountAmount(),
                    TransactionType.ROLLBACK_RESERVE
            );
            couponTransactionHistoryJpaRepository.save(rollbackHistory);

            log.info("쿠폰 재예약 완료 - orderId: {}, couponId: {}",
                    orderId, history.getCouponId());
        }

        log.info("===== 쿠폰 재예약 완료 (보상) ===== orderId: {}", orderId);
    }

    private Integer calculateDiscount(CouponPolicy policy, Integer productPrice) {
        Integer discount = 0;

        if (productPrice < policy.getMinimumOrderAmount()) {
            throw new CustomGlobalException(ErrorType.MINIMUM_ORDER_AMOUNT_NOT_MET);
        }

        if (policy.getDiscountType() == DiscountType.FIXED_DISCOUNT) {
            discount = policy.getDiscountValue();
        } else if (policy.getDiscountType() == DiscountType.RATE_DISCOUNT) {
            discount = (int) (productPrice * policy.getDiscountValue() / 100.0);
        }

        // 최대 할인 금액 제한
        if (discount > policy.getMaximumDiscountAmount()) {
            discount = policy.getMaximumDiscountAmount();
        }

        return discount;
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

    @Transactional(readOnly = true)
    public CouponResponse.Response getCoupon(Long couponId, Long userId) {
        Coupon coupon = couponRepository.findByIdAndUserId(couponId, userId)
                .orElseThrow(() -> new CustomGlobalException(ErrorType.COUPON_NOT_FOUND));
        return CouponResponse.Response.from(coupon);
    }

    public CouponValidationResponse validateCoupon(Long couponId, Long userId) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElse(null);

        if (coupon == null) {
            return CouponValidationResponse.builder()
                    .valid(false)
                    .invalidReason("쿠폰을 찾을 수 없습니다.")
                    .build();
        }

        CouponPolicy policy = coupon.getCouponPolicy();
        LocalDateTime now = LocalDateTime.now();

        // 사용자 검증
        if (!coupon.getUserId().equals(userId)) {
            return buildInvalidResponse("본인의 쿠폰이 아닙니다.");
        }

        // 상태 검증
        if (coupon.getStatus() != CouponStatus.AVAILABLE) {
            return buildInvalidResponse("사용 가능한 쿠폰이 아닙니다.");
        }

        // 시작 시간 검증
        if (now.isBefore(policy.getStartTime())) {
            return buildInvalidResponse("쿠폰 사용 시작 전입니다.");
        }

        // 만료 시간 검증
        if (now.isAfter(policy.getEndTime())) {
            return buildInvalidResponse("만료된 쿠폰입니다.");
        }

        // 검증 성공
        return CouponValidationResponse.builder()
                .valid(true)
                .couponPolicy(CouponValidationResponse.CouponPolicyDto.from(policy))
                .build();
    }

    private CouponValidationResponse buildInvalidResponse(String reason) {
        return CouponValidationResponse.builder()
                .valid(false)
                .invalidReason(reason)
                .build();
    }
}
