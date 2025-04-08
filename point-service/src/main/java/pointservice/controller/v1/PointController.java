package pointservice.controller.v1;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;
import pointservice.common.interceptor.UserIdInterceptor;
import pointservice.controller.dto.PointRequest;
import pointservice.controller.dto.PointResponse;
import pointservice.entity.Point;
import pointservice.service.v1.PointFacade;
import pointservice.service.v1.PointService;

@RestController
@RequestMapping("/api/v1/points")
@RequiredArgsConstructor
public class PointController {

    private final PointService pointService;
    private final PointFacade pointFacade;

    @PostMapping("/earn")
    public PointResponse earn(@Valid @RequestBody PointRequest.Earn request) {

        Long userId = UserIdInterceptor.getCurrentUserId();
        try {
            return PointResponse.from(pointFacade.earn(userId, request.getAmount()));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @PostMapping("/use")
    public PointResponse use(@Valid @RequestBody PointRequest.Use request) {

        Long userId = UserIdInterceptor.getCurrentUserId();
        return PointResponse.from(pointService.use(userId, request.getAmount()));
    }

    @PostMapping("/{pointId}/cancel")
    public PointResponse cancel(@PathVariable Long pointId) {

        return PointResponse.from(pointService.cancel(pointId));
    }

    @GetMapping("/users/{userId}/balance")
    public PointResponse.Balance getBalance(@PathVariable Long userId) {

        Long balance = pointService.getBalance(userId);
        return PointResponse.Balance.of(userId, balance);
    }

    @GetMapping("/users/{userId}/history")
    public Page<PointResponse> getPointHistory(
            @PathVariable Long userId,
            Pageable pageable) {

        Page<Point> points = pointService.getPointHistory(userId, pageable);
        Page<PointResponse> responses = points.map(PointResponse::from);
        return responses;
    }
}
