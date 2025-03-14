package pointservice.service.v2;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import pointservice.entity.PointBalance;
import pointservice.repository.PointBalanceJpaRepository;
import pointservice.repository.PointJpaRepository;
import pointservice.service.v1.PointFacade;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
class PointLockServiceIntegrationTest {

    @Autowired
    private PointFacade pointFacade;

    @Autowired
    private RedissonLockPointService redissonLockPointService;

    @Autowired
    private PointBalanceJpaRepository pointBalanceJpaRepository;

    @Autowired
    private PointJpaRepository pointJpaRepository;

    @BeforeEach
    void setUp() {
        // 테스트 전 데이터 초기화 (기존 데이터가 있다면 삭제)
        pointJpaRepository.deleteAll();
        pointBalanceJpaRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        // 테스트 후 데이터 정리
        pointJpaRepository.deleteAll();
        pointBalanceJpaRepository.deleteAll();
    }

    @Test
    @DisplayName("1명의 유저가 동시에 100번 포인트 적립을 하면 100 포인트가 적립되어야 한다.")
    void concurrentPointEarn() throws InterruptedException {
        // given
        Long userId = 1L;
        int threadCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger failCount = new AtomicInteger(0);

        // when
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    redissonLockPointService.earn(userId, 1L);
                }catch (Exception e) {
                    // 예외가 발생하면 카운트
                    failCount.incrementAndGet();
                    System.err.println("Error during point earn: " + e.getMessage());
                }finally {
                    latch.countDown();
                }
            });
        }

        // 모든 스레드가 작업을 완료할 때까지 대기
        latch.await();
        executorService.shutdown();

        System.out.println("Failed operations: " + failCount.get());

        // then
        PointBalance pointBalance = pointBalanceJpaRepository.findByUserIdNoLock(userId).orElseThrow();
        assertThat(pointBalance.getBalance()).isEqualTo(100L);

        // 추가 검증: Point 엔티티가 100개 생성되었는지 확인
        long pointCount = pointJpaRepository.count();
        assertThat(pointCount).isEqualTo(100L);
    }
}