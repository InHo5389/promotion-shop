package orderservice.client.serviceclient;

import feign.FeignException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import orderservice.client.PointClient;
import orderservice.client.dto.PointRequest;
import orderservice.client.dto.PointResponse;
import orderservice.common.exception.CustomGlobalException;
import orderservice.common.exception.ErrorType;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;

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
}
