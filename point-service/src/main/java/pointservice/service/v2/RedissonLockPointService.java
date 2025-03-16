package pointservice.service.v2;

import lombok.RequiredArgsConstructor;
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
        RLock lock = redissonClient.getLock(POINT_LOCK_PREFIX + userId);
        try {
            boolean locked = lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS);
            if (!locked) {
                throw new IllegalStateException("Failed to acquire lock for user: " + userId);
            }

            return pointService.earn(userId, amount);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("LOCK acquisition was interrupted");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    public Point use(Long userId, Long amount) {
        RLock lock = redissonClient.getLock(POINT_LOCK_PREFIX + userId);
        try {
            boolean locked = lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS);
            if (!locked) {
                throw new IllegalStateException("Failed to acquire lock for user: " + userId);
            }

            return pointService.use(userId, amount);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("LOCK acquisition was interrupted");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    public Point cancel(Long pointId) {
        Point point = pointRepository.findById(pointId)
                .orElseThrow(() -> new CustomGlobalException(ErrorType.NOT_FOUND_POINT));
        Long userId = point.getUserId();

        RLock lock = redissonClient.getLock(POINT_LOCK_PREFIX + userId);
        try {
            boolean locked = lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS);
            if (!locked) {
                throw new IllegalStateException("Failed to acquire lock for user: " + userId);
            }

            return pointService.cancel(point);
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
