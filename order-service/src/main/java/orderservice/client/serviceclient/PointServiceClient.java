package orderservice.client.serviceclient;

import feign.FeignException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import orderservice.client.PointClient;
import orderservice.client.dto.PointRequest;
import orderservice.client.dto.PointReserveRequest;
import orderservice.client.dto.PointResponse;
import orderservice.common.exception.CustomGlobalException;
import orderservice.common.exception.ErrorType;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PointServiceClient {

    private final PointClient pointClient;

    @CircuitBreaker(name = "pointService", fallbackMethod = "useFallback")
    public PointResponse use(PointRequest.Use request) {
        return pointClient.use(request);
    }

    public PointResponse useFallback(PointRequest.Use request, Exception ex) {
        log.error("Failed to use Point {}: {}", request, ex.getMessage());
        if (ex instanceof FeignException) {
            throw (FeignException) ex;
        }
        throw new CustomGlobalException(ErrorType.POINT_SERVICE_UNAVAILABLE);
    }

    @CircuitBreaker(name = "pointService", fallbackMethod = "earnFallback")
    public PointResponse earn(PointRequest.Earn request) {
        return pointClient.earn(request);
    }

    public PointResponse earnFallback(PointRequest.Earn request, Exception ex) {
        log.error("Failed to earn Point {}: {}", request.getAmount(), ex.getMessage());
        if (ex instanceof FeignException) {
            throw (FeignException) ex;
        }
        throw new CustomGlobalException(ErrorType.POINT_SERVICE_UNAVAILABLE);
    }

    @Retry(name = "pointService")
    @CircuitBreaker(name = "pointService", fallbackMethod = "reservePointsFallback")
    public void reservePoints(PointReserveRequest request) {
        log.info("포인트 예약 요청. orderId: {}", request.orderId());
        pointClient.reservePoints(request);
        log.info("포인트 예약 성공. orderId: {}", request.orderId());
    }

    public void reservePointsFallback(PointReserveRequest request, Exception ex) {
        log.error("Failed to earn Point {}: {}", request.amount(), ex.getMessage());
        if (ex instanceof FeignException) {
            throw (FeignException) ex;
        }
        throw new CustomGlobalException(ErrorType.POINT_SERVICE_UNAVAILABLE);
    }

    @Retry(name = "pointService")
    @CircuitBreaker(name = "pointService", fallbackMethod = "confirmPointsFallback")
    public void confirmPoints(Long orderId) {
        log.info("포인트 확정 요청. orderId: {}", orderId);
        pointClient.confirmPoints(orderId);
        log.info("포인트 확정 성공. orderId: {}", orderId);
    }

    private void confirmPointsFallback(Long orderId, Exception ex) {
        log.error("포인트 확정 실패 fallback. orderId: {}, error: {}", orderId, ex.getMessage());
        if (ex instanceof FeignException) {
            throw (FeignException) ex;
        }
        throw new CustomGlobalException(ErrorType.POINT_SERVICE_UNAVAILABLE);
    }

    @Retry(name = "pointService")
    @CircuitBreaker(name = "pointService", fallbackMethod = "cancelReservationFallback")
    public void cancelReservation(Long orderId) {
        log.info("포인트 취소 요청. orderId: {}", orderId);
        pointClient.cancelReservation(orderId);
        log.info("포인트 취소 성공. orderId: {}", orderId);
    }

    private void cancelReservationFallback(Long orderId, Exception ex) {
        log.error("포인트 취소 실패 fallback. orderId: {}, error: {}", orderId, ex.getMessage());
        log.warn("포인트 취소 실패했지만 계속 진행. orderId: {}", orderId);
    }

    @Retry(name = "pointService")
    @CircuitBreaker(name = "pointService", fallbackMethod = "rollbackConfirmPointsFallback")
    public void rollbackConfirmPoints(Long orderId) {
        log.info("포인트 확정 롤백 요청. orderId: {}", orderId);
        pointClient.rollbackConfirmPoints(orderId);
        log.info("포인트 확정 롤백 성공. orderId: {}", orderId);
    }

    private void rollbackConfirmPointsFallback(Long orderId, Exception ex) {
        log.error("포인트 확정 롤백 실패. orderId: {}, error: {}", orderId, ex.getMessage());
        log.warn("포인트 확정 롤백 실패했지만 계속 진행. orderId: {}", orderId);
    }

    @Retry(name = "pointService")
    @CircuitBreaker(name = "pointService", fallbackMethod = "rollbackReservePointsFallback")
    public void rollbackReservePoints(Long orderId) {
        log.info("포인트 확정 롤백 요청. orderId: {}", orderId);
        pointClient.rollbackReservePoints(orderId);
        log.info("포인트 확정 롤백 성공. orderId: {}", orderId);
    }

    private void rollbackReservePointsFallback(Long orderId, Exception ex) {
        log.error("포인트 확정 롤백 실패. orderId: {}, error: {}", orderId, ex.getMessage());
        log.warn("포인트 확정 롤백 실패했지만 계속 진행. orderId: {}", orderId);
    }
}
