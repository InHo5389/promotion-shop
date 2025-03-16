package pointservice.service.v1;

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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
class PointServiceIntegrationTest {

    @Autowired
    private PointFacade pointFacade;

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

        // when
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    pointFacade.earn(userId, 1L);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                } finally {
                    latch.countDown();
                }
            });
        }

        // 모든 스레드가 작업을 완료할 때까지 대기
        latch.await();
        executorService.shutdown();

        // then
        PointBalance pointBalance = pointBalanceJpaRepository.findByUserIdNoLock(userId).orElseThrow();
        assertThat(pointBalance.getBalance()).isEqualTo(100L);

        // 추가 검증: Point 엔티티가 100개 생성되었는지 확인
        long pointCount = pointJpaRepository.count();
        assertThat(pointCount).isEqualTo(100L);
    }
}