package orderservice.common.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import orderservice.client.serviceclient.CouponServiceClient;
import orderservice.client.serviceclient.PointServiceClient;
import orderservice.client.serviceclient.ProductServiceClient;
import orderservice.entity.CompensationRegistry;
import orderservice.repository.CompensationRegistryJpaRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CompensationRetryScheduler {

    private final CompensationRegistryJpaRepository compensationRegistryRepository;
    private final ProductServiceClient productClient;
    private final CouponServiceClient couponClient;
    private final PointServiceClient pointClient;

    @Scheduled(fixedDelay = 60000, initialDelay = 10000)  // 1분마다, 시작 10초 후
    public void retryPendingCompensations() {
        log.info("===== 보상 트랜잭션 재시도 시작 =====");

        List<CompensationRegistry> pendingList = compensationRegistryRepository
                .findByStatusOrderByCreatedAtAsc(CompensationRegistry.CompensationStatus.PENDING);

        if (pendingList.isEmpty()) {
            log.info("재시도 대상 없음");
            return;
        }

        log.info("재시도 대상: {}건", pendingList.size());

        int successCount = 0;
        int failCount = 0;

        for (CompensationRegistry registry : pendingList) {
            boolean success = retryCompensation(registry);
            if (success) {
                successCount++;
            } else {
                failCount++;
            }
        }

        log.info("===== 보상 트랜잭션 재시도 완료 ===== 성공: {}건, 실패: {}건", successCount, failCount);
    }

    @Transactional
    public boolean retryCompensation(CompensationRegistry registry) {
        Long orderId = registry.getOrderId();
        CompensationRegistry.CompensationType type = registry.getType();

        log.info("보상 재시도 - orderId: {}, type: {}", orderId, type);

        try {
            switch (type) {
                case ORDER_CREATE_ROLLBACK -> retryOrderCreateRollback(orderId);
                case ORDER_CANCEL_PENDING -> retryOrderCancelPending(orderId);
                case ORDER_CANCEL_CONFIRMED -> retryOrderCancelConfirmed(orderId);
            }

            registry.markCompleted();
            compensationRegistryRepository.save(registry);

            log.info("보상 재시도 성공 - orderId: {}, type: {}", orderId, type);
            return true;

        } catch (Exception e) {
            log.error("보상 재시도 실패 - orderId: {}, type: {} (다음 스케줄에 재시도)", orderId, type, e);
            return false;
        }
    }

    private void retryOrderCreateRollback(Long orderId) {
        log.info("주문 생성 실패 보상 - 예약 취소 시도 - orderId: {}", orderId);

        productClient.cancelReservation(orderId);
        log.info("재고 예약 취소 완료 - orderId: {}", orderId);

        couponClient.cancelReservation(orderId);
        log.info("쿠폰 예약 취소 완료 - orderId: {}", orderId);

        pointClient.cancelReservation(orderId);
        log.info("포인트 예약 취소 완료 - orderId: {}", orderId);
    }

    /**
     * PENDING 주문 취소 실패 보상: 재예약
     */
    private void retryOrderCancelPending(Long orderId) {
        log.info("PENDING 주문 취소 실패 보상 - 재예약 시도 - orderId: {}", orderId);

        productClient.rollbackReserveStock(orderId);
        log.info("재고 재예약 완료 - orderId: {}", orderId);

        couponClient.rollbackReserveCoupon(orderId);
        log.info("쿠폰 재예약 완료 - orderId: {}", orderId);

        pointClient.rollbackReservePoints(orderId);
        log.info("포인트 재예약 완료 - orderId: {}", orderId);
    }

    /**
     * CONFIRMED 주문 취소 실패 보상: 재확정
     */
    private void retryOrderCancelConfirmed(Long orderId) {
        log.info("CONFIRMED 주문 취소 실패 보상 - 재확정 시도 - orderId: {}", orderId);

        productClient.confirmStock(orderId);
        log.info("재고 재확정 완료 - orderId: {}", orderId);

        couponClient.confirmCoupon(orderId);
        log.info("쿠폰 재확정 완료 - orderId: {}", orderId);

        pointClient.confirmPoints(orderId);
        log.info("포인트 재확정 완료 - orderId: {}", orderId);
    }
}
