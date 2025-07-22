package orderservice.client.serviceclient;

import feign.FeignException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import orderservice.client.CouponClient;
import orderservice.client.dto.CouponResponse;
import orderservice.common.exception.CustomGlobalException;
import orderservice.common.exception.ErrorType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponServiceClient {

    private final CouponClient couponClient;

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

    @CircuitBreaker(name = "couponService", fallbackMethod = "cancelCouponFallback")
    public ResponseEntity<CouponResponse.Response> cancelCoupon(Long couponId) {
        return couponClient.cancelCoupon(couponId);
    }

    private CouponResponse.Response cancelCouponFallback(Long couponId, Exception ex) {
        log.error("Failed to cancel coupon {}: userId: {}, {}", couponId, ex.getMessage());

        if (ex instanceof FeignException) {
            throw (FeignException) ex;
        }

        throw new CustomGlobalException(ErrorType.COUPON_SERVICE_UNAVAILABLE);
    }
}
