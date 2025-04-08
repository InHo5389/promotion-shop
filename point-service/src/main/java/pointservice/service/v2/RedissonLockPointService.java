package pointservice.service.v2;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import pointservice.common.exception.CustomGlobalException;
import pointservice.common.exception.ErrorType;
import pointservice.entity.Point;
import pointservice.service.PointRepository;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedissonLockPointService {

    private final PointService pointService;
    private final PointRepository pointRepository;
    private final RedissonClient redissonClient;

    private static final String POINT_LOCK_PREFIX = "point:lock:";
    private static final long LOCK_WAIT_TIME = 5L;
    private static final long LOCK_LEASE_TIME = 5L;

    public Point earn(Long userId, Long amount) {
        log.info("분산 락을 이용한 포인트 적립 시작 - userId: {}, amount: {}P", userId, amount);

        int retries = 0;
        while (retries < 3) {  // 최대 3번 재시도
            RLock lock = redissonClient.getLock(POINT_LOCK_PREFIX + userId);
            try {
                boolean locked = lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS);
                if (locked) {
                    log.debug("포인트 락 획득 성공 - userId: {}", userId);

                    Point result = pointService.earn(userId, amount);
                    log.info("분산 락을 이용한 포인트 적립 완료 - userId: {}, pointId: {}", userId, result.getId());
                    return result;
                }

                // 락 획득 실패 시 재시도
                log.warn("포인트 락 획득 실패, 재시도 - userId: {}, 시도 횟수: {}/{}", userId, retries + 1, 3);
                retries++;
                Thread.sleep(100 * retries);  // 지수 백오프
            } catch (InterruptedException e) {
                log.error("포인트 락 획득 중 인터럽트 발생 - userId: {}", userId, e);
                Thread.currentThread().interrupt();
                throw new IllegalStateException("LOCK acquisition was interrupted");
            } finally {
                if (lock != null && lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
        }

        log.error("최대 재시도 횟수 초과로 포인트 적립 실패 - userId: {}, amount: {}P", userId, amount);
        throw new IllegalStateException("Failed to acquire lock after " + retries + " retries");
    }

    public Point use(Long userId, Long amount) {
        log.info("분산 락을 이용한 포인트 사용 시작 - userId: {}, amount: {}P", userId, amount);

        RLock lock = redissonClient.getLock(POINT_LOCK_PREFIX + userId);
        try {
            log.debug("포인트 락 획득 시도 - userId: {}", userId);
            boolean locked = lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS);
            if (!locked) {
                log.error("포인트 락 획득 실패 - userId: {}", userId);
                throw new IllegalStateException("Failed to acquire lock for user: " + userId);
            }

            log.debug("포인트 락 획득 성공 - userId: {}", userId);
            Point result = pointService.use(userId, amount);
            log.info("분산 락을 이용한 포인트 사용 완료 - userId: {}, pointId: {}", userId, result.getId());
            return result;
        } catch (InterruptedException e) {
            log.error("포인트 락 획득 중 인터럽트 발생 - userId: {}", userId, e);
            Thread.currentThread().interrupt();
            throw new IllegalStateException("LOCK acquisition was interrupted");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.debug("포인트 락 해제 - userId: {}", userId);
            }
        }
    }

    public Point cancel(Long pointId) {
        log.info("분산 락을 이용한 포인트 취소 시작 - pointId: {}", pointId);

        Point point = pointRepository.findById(pointId)
                .orElseThrow(() -> new CustomGlobalException(ErrorType.NOT_FOUND_POINT));
        Long userId = point.getUserId();

        RLock lock = redissonClient.getLock(POINT_LOCK_PREFIX + userId);
        try {
            boolean locked = lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS);
            if (!locked) {
                throw new IllegalStateException("Failed to acquire lock for user: " + userId);
            }

            log.debug("포인트 락 획득 성공 - userId: {}, pointId: {}", userId, pointId);
            Point result = pointService.cancel(point);
            log.info("분산 락을 이용한 포인트 취소 완료 - userId: {}, pointId: {}", userId, pointId);
            return result;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("LOCK acquisition was interrupted");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    public Long getBalance(Long userId) {
        return pointService.getBalance(userId);
    }

    public Page<Point> getPointHistory(Long userId, Pageable pageable) {
        return pointService.getPointHistory(userId, pageable);
    }
}
