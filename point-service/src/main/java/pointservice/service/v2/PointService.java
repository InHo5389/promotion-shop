package pointservice.service.v2;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
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
        log.info("포인트 적립 요청 - userId: {}, amount: {}P", userId, amount);

        Long balance = pointRedisRepository.getBalanceFormCache(userId);
        if (balance == null) {
            log.debug("캐시에서 잔액 조회 실패, DB에서 조회 - userId: {}", userId);

            balance = getBalanceDb(userId);
            pointRedisRepository.updateBalanceCache(userId, balance);

            log.debug("캐시 업데이트 완료 - userId: {}, balance: {}P", userId, balance);
        }

        PointBalance pointBalance = pointBalanceRepository.findByUserId(userId)
                .orElseGet(() -> {
                    log.info("신규 포인트 계정 생성 - userId: {}", userId);
                    return PointBalance.create(userId);
                });
        pointBalance.earn(amount);
        PointBalance savedPointBalance = pointBalanceRepository.save(pointBalance);

        pointRedisRepository.updateBalanceCache(userId, savedPointBalance.getBalance());

        Point point = Point.create(userId, amount, PointType.EARNED, savedPointBalance.getBalance(), savedPointBalance);
        Point savedPoint = pointRepository.save(point);

        log.info("포인트 적립 완료 - userId: {}, 적립액: {}P, 이전 잔액: {}P, 현재 잔액: {}P",
                userId, amount, pointBalance.getBalance(), savedPointBalance.getBalance());
        return savedPoint;
    }

    private Long getBalanceDb(Long userId) {
        log.debug("DB에서 포인트 잔액 조회 - userId: {}", userId);

        return pointBalanceRepository.findByUserId(userId)
                .map(PointBalance::getBalance)
                .orElse(0L);
    }

    @Transactional
    public Point use(Long userId, Long amount) {
        log.info("포인트 사용 요청 - userId: {}, amount: {}P", userId, amount);

        Long balance = pointRedisRepository.getBalanceFormCache(userId);
        if (balance == null) {
            balance = getBalanceDb(userId);
            pointRedisRepository.updateBalanceCache(userId, balance);
        }

        if (balance < amount) {
            log.info("포인트 잔액 부족 - userId: {}, 요청 금액: {}P, 현재 잔액: {}P", userId, amount, balance);
            throw new CustomGlobalException(ErrorType.INSUFFICIENT_POINT_BALANCE);
        }

        if (amount % 10 == 0 && amount % 100 != 0) {
            log.info("포인트 10원 단위 사용 불가 - userId: {}, 요청 금액: {}P", userId, amount);
            throw new CustomGlobalException(ErrorType.INVALID_POINT_AMOUNT);
        }

        PointBalance pointBalance = pointBalanceRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomGlobalException(ErrorType.NOT_FOUND_POINT_BALANCE));
        Long beforeBalance = pointBalance.getBalance();

        pointBalance.use(amount);

        pointRedisRepository.updateBalanceCache(userId, pointBalance.getBalance());

        Point point = Point.create(userId, amount, PointType.USED, pointBalance.getBalance(), pointBalance);

        log.info("포인트 사용 완료 - userId: {}, 사용액: {}P, 이전 잔액: {}P, 현재 잔액: {}P",
                userId, amount, pointBalance, pointBalance.getBalance());
        return pointRepository.save(point);
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public Point cancel(Point point) {
        log.info("포인트 취소 요청 - userId: {}, pointId: {}, amount: {}P, type: {}",
                point.getUserId(), point.getId(), point.getAmount(), point.getType());

        point.cancel();

        PointBalance pointBalance = point.getPointBalance();
        pointRedisRepository.updateBalanceCache(point.getUserId(), pointBalance.getBalance());

        Point savedPoint = Point.create(point.getUserId(), point.getAmount(), PointType.CANCELED, pointBalance.getBalance(), pointBalance);

        return pointRepository.save(savedPoint);
    }

    @Transactional(readOnly = true)
    public Long getBalance(Long userId) {
        log.info("포인트 잔액 조회 요청 - userId: {}", userId);

        Long balance = pointRedisRepository.getBalanceFormCache(userId);
        if (balance != null){
            return balance;
        }

        Long balanceDb = getBalanceDb(userId);
        pointRedisRepository.updateBalanceCache(userId,balanceDb);
        log.debug("캐시 업데이트 완료 - userId: {}, balance: {}P", userId, balanceDb);

        return balanceDb;
    }

    public Page<Point> getPointHistory(Long userId, Pageable pageable) {
        log.info("포인트 이력 조회 - userId: {}, page: {}, size: {}",
                userId, pageable.getPageNumber(), pageable.getPageSize());
        return pointRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }
}
