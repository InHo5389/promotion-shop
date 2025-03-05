package productservice.facade;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;
import productservice.service.ProductService;
import productservice.service.dto.ProductOptionRequest;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedissonLockIncreaseFacade {
    private final RedissonClient redissonClient;
    private final ProductService productService;

    private static final String LOCK_KEY_PREFIX = "PRODUCT_STOCK_LOCK:";

    public void increaseStock(List<ProductOptionRequest.StockUpdate> requests) {
        if (requests.isEmpty()) return;

        for (ProductOptionRequest.StockUpdate request : requests) {
            increaseStockForSingleOption(request);
        }
    }

    private void increaseStockForSingleOption(ProductOptionRequest.StockUpdate request) {
        String lockKey = LOCK_KEY_PREFIX + request.getOptionId();
        RLock lock = redissonClient.getLock(lockKey);

        try {
            boolean isLocked = lock.tryLock(10, 3, TimeUnit.SECONDS);

            if (!isLocked) {
                throw new RuntimeException("재고 업데이트를 위한 락 획득 실패");
            }

            try {
                productService.increaseStock(List.of(request));
            } finally {
                if (lock.isLocked() && lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
        } catch (InterruptedException e) {
            log.error("락 획득 중 인터럽트 발생", e);
            Thread.currentThread().interrupt();
            throw new RuntimeException("락 획득 중 인터럽트 발생", e);
        }
    }
}
