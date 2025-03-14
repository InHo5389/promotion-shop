package pointservice.service.v2;

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
import pointservice.service.PointBalanceRepository;
import pointservice.service.PointRepository;

@Service("pointServiceV2")
@RequiredArgsConstructor
public class PointService {

    private final PointRepository pointRepository;
    private final PointBalanceRepository pointBalanceRepository;
    private final PointRedisRepository pointRedisRepository;

    /**
     * 분산 락 획득, 캐시됭 잔액 조회 (없으면 DB 조회), 포인트 잔액 증가
     * DB 저장 및 캐시 업데이트, 포인트 이력 저장
     */
    @Transactional
    public Point earn(Long userId, Long amount) {
        Long balance = pointRedisRepository.getBalanceFormCache(userId);
        if (balance == null) {
            balance = getBalanceDb(userId);
            pointRedisRepository.updateBalanceCache(userId, balance);
        }

        PointBalance pointBalance = pointBalanceRepository.findByUserId(userId)
                .orElseGet(() -> PointBalance.create(userId));
        pointBalance.earn(amount);
        PointBalance savedPointBalance = pointBalanceRepository.save(pointBalance);

        pointRedisRepository.updateBalanceCache(userId, savedPointBalance.getBalance());

        Point point = Point.create(userId, amount, PointType.EARNED, savedPointBalance.getBalance(), savedPointBalance);
        return pointRepository.save(point);
    }

    private Long getBalanceDb(Long userId) {
        return pointBalanceRepository.findByUserId(userId)
                .map(PointBalance::getBalance)
                .orElse(0L);
    }

    @Transactional
    public Point use(Long userId, Long amount) {
        Long balance = pointRedisRepository.getBalanceFormCache(userId);
        if (balance == null) {
            balance = getBalanceDb(userId);
            pointRedisRepository.updateBalanceCache(userId, balance);
        }

        if (balance < amount) {
            throw new CustomGlobalException(ErrorType.INSUFFICIENT_POINT_BALANCE);
        }

        PointBalance pointBalance = pointBalanceRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomGlobalException(ErrorType.NOT_FOUND_POINT_BALANCE));
        pointBalance.use(amount);

        pointRedisRepository.updateBalanceCache(userId, pointBalance.getBalance());

        Point point = Point.create(userId, amount, PointType.USED, pointBalance.getBalance(), pointBalance);
        return pointRepository.save(point);
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public Point cancel(Point point) {
        point.cancel();

        PointBalance pointBalance = point.getPointBalance();
        pointRedisRepository.updateBalanceCache(point.getUserId(), pointBalance.getBalance());

        Point savedPoint = Point.create(point.getUserId(), point.getAmount(), PointType.CANCELED, pointBalance.getBalance(), pointBalance);

        return pointRepository.save(savedPoint);
    }

    @Transactional(readOnly = true)
    public Long getBalance(Long userId) {
        Long balance = pointRedisRepository.getBalanceFormCache(userId);
        if (balance != null){
            return balance;
        }

        Long balanceDb = getBalanceDb(userId);
        pointRedisRepository.updateBalanceCache(userId,balanceDb);

        return balanceDb;
    }

    public Page<Point> getPointHistory(Long userId, Pageable pageable) {
        return pointRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }
}
