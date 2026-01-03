package pointservice.service.v1;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import pointservice.common.exception.CustomGlobalException;
import pointservice.common.exception.ErrorType;
import pointservice.entity.Point;
import pointservice.entity.PointBalance;
import pointservice.entity.PointType;
import pointservice.repository.PointJpaRepository;
import pointservice.service.PointBalanceRepository;
import pointservice.service.PointRepository;

@Service
@RequiredArgsConstructor
public class PointService {

    private final PointJpaRepository pointRepository;
    private final PointBalanceRepository pointBalanceRepository;

    @Transactional
    public Point earn(Long userId, Long amount) {
        PointBalance pointBalance = pointBalanceRepository.findByUserId(userId)
                .orElseGet(() -> PointBalance.create(userId));

        pointBalance.earn(amount);
        PointBalance savedPointBalance = pointBalanceRepository.save(pointBalance);

        Point point = Point.create(userId, amount, PointType.EARNED, pointBalance.getBalance(), savedPointBalance);
        return pointRepository.save(point);
    }

    @Transactional
    public Point use(Long userId, Long amount) {
        PointBalance pointBalance = pointBalanceRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomGlobalException(ErrorType.NOT_FOUND_POINT_BALANCE));

        pointBalance.use(amount);

        Point point = Point.create(userId, amount, PointType.USED, pointBalance.getBalance(), pointBalance);
        return pointRepository.save(point);
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public Point cancel(Long pointId) {
        Point point = pointRepository.findById(pointId)
                .orElseThrow(() -> new CustomGlobalException(ErrorType.NOT_FOUND_POINT));
        point.cancel();

        return point;
    }

    @Transactional(readOnly = true)
    public Long getBalance(Long userId) {
        return pointBalanceRepository.findByUserId(userId)
                .map(PointBalance::getBalance)
                .orElse(0L);
    }

    public Page<Point> getPointHistory(Long userId, Pageable pageable) {
        return pointRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }
}
