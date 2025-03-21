package orderservice.client.serviceclient;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import orderservice.client.CouponClient;
import orderservice.client.dto.CouponRequest;
import orderservice.client.dto.CouponResponse;
import orderservice.common.exception.CustomGlobalException;
import orderservice.common.exception.ErrorType;
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
        throw new CustomGlobalException(ErrorType.COUPON_SERVICE_UNAVAILABLE);
    }

    @CircuitBreaker(name = "couponService", fallbackMethod = "useCouponFallback")
    public CouponResponse.Response use(Long couponId, CouponRequest.Use request) {
        return couponClient.use(couponId, request);
    }

    private CouponResponse.Response useCouponFallback(Long couponId, CouponRequest.Use request, Exception ex) {
        log.error("Failed to use coupon {}: {}", couponId, ex.getMessage());
        throw new CustomGlobalException(ErrorType.COUPON_SERVICE_UNAVAILABLE);
    }
}
