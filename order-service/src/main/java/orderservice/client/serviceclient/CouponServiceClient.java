package orderservice.client.serviceclient;

import feign.FeignException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import orderservice.client.CouponClient;
import orderservice.client.dto.CouponReserveRequest;
import orderservice.client.dto.CouponReserveResponse;
import orderservice.client.dto.CouponResponse;
import orderservice.client.dto.CouponValidationResponse;
import orderservice.common.exception.CustomGlobalException;
import orderservice.common.exception.ErrorType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponServiceClient {

    private final CouponClient couponClient;

    @Retry(name = "couponService")
    @CircuitBreaker(name = "couponService", fallbackMethod = "getCouponFallback")
    public CouponResponse.Response getCoupon(Long couponId) {
        return couponClient.getCoupon(couponId);
    }

    private CouponResponse.Response getCouponFallback(Long couponId, Exception ex) {
        log.error("Failed to get coupon {}: {}", couponId, ex.getMessage());
        if (ex instanceof FeignException) {
            throw (FeignException) ex;
        }
        throw new CustomGlobalException(ErrorType.COUPON_SERVICE_UNAVAILABLE);
    }

    @Retry(name = "couponService")
    @CircuitBreaker(name = "couponService", fallbackMethod = "useCouponFallback")
    public ResponseEntity<CouponResponse.Response> useCoupon(
            Long couponId,
            Long orderId
    ) {
        return couponClient.useCoupon(couponId, orderId);
    }

    private ResponseEntity<CouponResponse.Response> useCouponFallback(
            Long couponId,
            Long orderId,
            Exception ex) {
        log.error("Failed to use coupon {}: orderId: {} {}", couponId, orderId, ex.getMessage());
        if (ex instanceof FeignException) {
            throw (FeignException) ex;
        }
        throw new CustomGlobalException(ErrorType.COUPON_SERVICE_UNAVAILABLE);
    }

    @Retry(name = "couponService")
    @CircuitBreaker(name = "couponService", fallbackMethod = "getCouponFallback")
    public CouponResponse.Response getCoupon(Long couponId, Long userId) {
        return couponClient.getCoupon(couponId, userId);
    }

    private CouponResponse.Response getCouponFallback(Long couponId, Long userId, Exception ex) {
        log.error("Failed to get coupon {}: userId: {}, {}", couponId, userId, ex.getMessage());

        // FeignException이면 그대로 다시 던지기
        if (ex instanceof FeignException) {
            throw (FeignException) ex;
        }

        // 다른 예외만 CustomGlobalException으로 변환
        throw new CustomGlobalException(ErrorType.COUPON_SERVICE_UNAVAILABLE);
    }

    @Retry(name = "couponService",fallbackMethod = "retryFallback")
    @CircuitBreaker(name = "couponService", fallbackMethod = "reserveCouponFallback")
    public CouponReserveResponse reserveCoupon(CouponReserveRequest request) {
        log.info("쿠폰 예약 요청. orderId: {}", request.orderId());
        CouponReserveResponse response = couponClient.reserveCoupon(request).getBody();
        log.info("쿠폰 예약 성공. orderId: {}, totalDiscount: {}", request.orderId(), response.totalDiscount());
        return response;
    }

    private CouponReserveResponse retryFallback(CouponReserveRequest request, Exception ex) {
        log.error("=== RETRY FALLBACK === orderId: {}, 예외: {}",
                request.orderId(), ex.getClass().getSimpleName());
        throw new CustomGlobalException(ErrorType.COUPON_SERVICE_UNAVAILABLE);
    }

    private CouponReserveResponse reserveCouponFallback(CouponReserveRequest request, Exception ex) {
        log.error("쿠폰 예약 실패 fallback. orderId: {}, error: {}", request.orderId(), ex.getMessage());
        if (ex instanceof FeignException) {
            throw (FeignException) ex;
        }
        throw new CustomGlobalException(ErrorType.COUPON_SERVICE_UNAVAILABLE);
    }

    @Retry(name = "couponService")
    @CircuitBreaker(name = "couponService", fallbackMethod = "confirmCouponFallback")
    public void confirmCoupon(Long orderId) {
        log.info("쿠폰 확정 요청. orderId: {}", orderId);
        couponClient.confirmCoupon(orderId);
        log.info("쿠폰 확정 성공. orderId: {}", orderId);
    }

    private void confirmCouponFallback(Long orderId, Exception ex) {
        log.error("쿠폰 확정 실패 fallback. orderId: {}, error: {}", orderId, ex.getMessage());
        if (ex instanceof FeignException) {
            throw (FeignException) ex;
        }
        throw new CustomGlobalException(ErrorType.COUPON_SERVICE_UNAVAILABLE);
    }

    @Retry(name = "couponService")
    @CircuitBreaker(name = "couponService", fallbackMethod = "cancelReservationFallback")
    public void cancelReservation(Long orderId) {
        log.info("쿠폰 취소 요청. orderId: {}", orderId);
        couponClient.cancelReservation(orderId);
        log.info("쿠폰 취소 성공. orderId: {}", orderId);
    }

    private void cancelReservationFallback(Long orderId, Exception ex) {
        log.error("쿠폰 취소 실패 fallback. orderId: {}, error: {}", orderId, ex.getMessage());
        log.warn("쿠폰 취소 실패했지만 계속 진행. orderId: {}", orderId);
    }

    @Retry(name = "couponService")
    @CircuitBreaker(name = "couponService", fallbackMethod = "rollbackConfirmCouponFallback")
    public void rollbackConfirmCoupon(Long orderId) {
        log.info("쿠폰 확정 롤백 요청. orderId: {}", orderId);
        couponClient.rollbackConfirmCoupon(orderId);
        log.info("쿠폰 확정 롤백 성공. orderId: {}", orderId);
    }

    private void rollbackConfirmCouponFallback(Long orderId, Exception ex) {
        log.error("쿠폰 확정 롤백 실패. orderId: {}, error: {}", orderId, ex.getMessage());
        log.warn("쿠폰 확정 롤백 실패했지만 계속 진행. orderId: {}", orderId);
    }

    @Retry(name = "couponService")
    @CircuitBreaker(name = "couponService", fallbackMethod = "rollbackReserveCouponFallback")
    public void rollbackReserveCoupon(Long orderId) {
        log.info("쿠폰 예약 롤백 요청. orderId: {}", orderId);
        couponClient.rollbackReserveCoupon(orderId);
        log.info("쿠폰 예약 롤백 성공. orderId: {}", orderId);
    }

    private void rollbackReserveCouponFallback(Long orderId, Exception ex) {
        log.error("쿠폰 예약 롤백 실패. orderId: {}, error: {}", orderId, ex.getMessage());
        log.warn("쿠폰 예약 롤백 실패했지만 계속 진행. orderId: {}", orderId);
    }

    @Retry(name = "couponService")
    @CircuitBreaker(name = "couponService", fallbackMethod = "validateCouponFallback")
    public CouponValidationResponse validateCoupon(Long couponId, Long userId) {
        return couponClient.validateCoupon(couponId, userId);
    }

    private CouponValidationResponse validateCouponFallback(Long couponId, Long userId, Exception ex) {
        log.warn("validateCouponFallback. couponId: {}, userId: {}", couponId, userId);

        return CouponValidationResponse.builder()
                .valid(false)
                .invalidReason("쿠폰 서비스를 일시적으로 사용할 수 없습니다. {}" + ex)
                .build();
    }
}
