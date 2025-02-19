package couponservice.repository.v2;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Repository;

import java.util.concurrent.TimeUnit;

@Slf4j
@Repository
@RequiredArgsConstructor
public class CouponLockRepository {

    private final RedissonClient redissonClient;

    private static final String COUPON_LOCK_KEY = "coupon:lock:%s";
    private static final long LOCK_WAIT_TIME = 3;
    private static final long LOCK_LEASE_TIME = 5;

    public RLock getLock(Long policyId) {
        String lockKey = generateLockKey(policyId);
        return redissonClient.getLock(lockKey);
    }

    public boolean tryLock(RLock lock) {
        try {
            return lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.error("Lock acquisition failed: {}", e.getMessage());
            Thread.currentThread().interrupt();
            return false;
        }
    }

    public void unlock(RLock lock) {
        if (lock != null && lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }

    private String generateLockKey(Long policyId) {
        return String.format(COUPON_LOCK_KEY, policyId);
    }
}