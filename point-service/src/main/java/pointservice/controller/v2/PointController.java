package pointservice.controller.v2;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pointservice.common.interceptor.UserIdInterceptor;
import pointservice.controller.dto.PointRequest;
import pointservice.controller.dto.PointResponse;
import pointservice.entity.Point;
import pointservice.service.dto.PointReserveRequest;
import pointservice.service.v2.PointService;
import pointservice.service.v2.RedissonLockPointService;

@RestController("pointControllerV2")
@RequestMapping("/api/v2/points")
@RequiredArgsConstructor
public class PointController {

    private final RedissonLockPointService redissonLockPointService;
    private final PointService pointService;

    @PostMapping("/earn")
    public PointResponse earn(@Valid @RequestBody PointRequest.Earn request) {

        Long userId = UserIdInterceptor.getCurrentUserId();
        return PointResponse.from(redissonLockPointService.earn(userId, request.getAmount()));
    }

    @PostMapping("/reserve")
    public ResponseEntity<Void> reservePoints(@RequestBody PointReserveRequest request) {
        pointService.reservePoints(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/confirm/{orderId}")
    public ResponseEntity<Void> confirmPoints(
            @PathVariable Long orderId,
            @RequestHeader("X-USER-ID") Long userId
    ) {
        pointService.confirmReservation(orderId, userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/cancel/{orderId}")
    public ResponseEntity<Void> cancelReservation(
            @PathVariable Long orderId,
            @RequestHeader("X-USER-ID") Long userId
    ) {
        pointService.cancelReservation(orderId, userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/rollback/confirm/{orderId}")
    public ResponseEntity<Void> rollbackConfirmation(
            @PathVariable Long orderId,
            @RequestHeader("X-USER-ID") Long userId
    ) {
        pointService.rollbackConfirmPoints(orderId, userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/rollback/reserve/{orderId}")
    public ResponseEntity<Void> rollbackReservation(
            @PathVariable Long orderId,
            @RequestHeader("X-USER-ID") Long userId
    ) {
        pointService.rollbackReservePoints(orderId, userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/use")
    public PointResponse use(@Valid @RequestBody PointRequest.Use request) {

        Long userId = UserIdInterceptor.getCurrentUserId();
        return PointResponse.from(redissonLockPointService.use(userId, request.getAmount()));
    }

    @PostMapping("/usePoints")
    public void use(@Valid @RequestBody PointReserveRequest request) {

        pointService.usePoints(request);
    }

    @PostMapping("/{pointId}/cancel")
    public PointResponse cancel(@PathVariable Long pointId) {

        return PointResponse.from(redissonLockPointService.cancel(pointId));
    }

    @GetMapping("/users/{userId}/balance")
    public PointResponse.Balance getBalance(@PathVariable Long userId) {

        Long balance = redissonLockPointService.getBalance(userId);
        return PointResponse.Balance.of(userId, balance);
    }

    @GetMapping("/users/{userId}/history")
    public Page<PointResponse> getPointHistory(
            @PathVariable Long userId,
            Pageable pageable) {

        Page<Point> points = redissonLockPointService.getPointHistory(userId, pageable);
        Page<PointResponse> responses = points.map(PointResponse::from);
        return responses;
    }
}
