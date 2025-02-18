package couponservice.service.v1;

import couponservice.common.exception.CustomGlobalException;
import couponservice.common.exception.ErrorType;
import couponservice.common.interceptor.UserIdInterceptor;
import couponservice.entity.CouponPolicy;
import couponservice.entity.DiscountType;
import couponservice.repository.CouponPolicyRepository;
import couponservice.repository.CouponRepository;
import couponservice.service.dto.v1.CouponRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
class CouponServiceIntegrationTest {

    @Autowired
    private CouponService couponService;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private CouponPolicyRepository couponPolicyRepository;

    @Test
    @DisplayName("100명의 사용자가 동시에 쿠폰을 발급받으려고 할 때, 총 발행량을 초과하여 발급되지 않아야 한다")
    void issueCouponConcurrency() throws InterruptedException {
        //given
        int numberOfThreads = 100;
        int totalQuantity = 50;

        CouponPolicy savedCouponPolicy = couponPolicyRepository.save(
                CouponPolicy.builder()
                        .title("테스트 쿠폰")
                        .discountType(DiscountType.FIXED_DISCOUNT)
                        .discountValue(1000)
                        .minimumOrderAmount(10000)
                        .maximumDiscountAmount(1000)
                        .totalQuantity(totalQuantity)
                        .startTime(LocalDateTime.now().minusDays(1))
                        .endTime(LocalDateTime.now().plusDays(1))
                        .build()
        );

        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);
        AtomicInteger successfulIssues = new AtomicInteger(0);

        // when
        for (int i = 0; i < numberOfThreads; i++) {
            Long userId = (long) i;
            executorService.submit(() -> {
                try {
                    UserIdInterceptor.setTestUserId(userId);  // ThreadLocal에 직접 설정

                    CouponRequest.Issue request = CouponRequest.Issue.builder()
                            .couponPolicyId(savedCouponPolicy.getId())
                            .build();

                    couponService.issue(request);
                    successfulIssues.incrementAndGet();
                } catch (CustomGlobalException e) {
                    if (!e.getErrorType().equals(ErrorType.COUPON_QUANTITY_EXHAUSTED)) {
                        throw e;
                    }
                } finally {
                    UserIdInterceptor.clearTestUserId();  // ThreadLocal 정리
                    latch.countDown();
                }
            });
        }

        latch.await();

        //then
        Long actualIssuedCount = couponRepository.countByCouponPolicyId(savedCouponPolicy.getId());
        assertThat(actualIssuedCount).isEqualTo(totalQuantity);
        assertThat(successfulIssues.get()).isEqualTo(totalQuantity);
    }

}