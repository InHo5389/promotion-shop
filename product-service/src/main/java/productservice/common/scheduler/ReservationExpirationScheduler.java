package productservice.common.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import productservice.entity.ProductTransactionHistory;
import productservice.repository.ProductTransactionJpaRepository;
import productservice.service.ProductService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static productservice.entity.ProductTransactionHistory.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationExpirationScheduler {

    private final ProductService productService;
    private final ProductTransactionJpaRepository productTransactionJpaRepository;

    @Scheduled(fixedRate = 600000)
    public void cancelExpiredReservations() {
        log.info("예약 만료 스케줄러 시작");

        LocalDateTime expireTime = LocalDateTime.now().minusMinutes(10);

        // 10분 지난 예약 찾기 (확정/취소 안 된 것들)
        List<ProductTransactionHistory> expiredReservations =
                productTransactionJpaRepository.findExpiredReservations(TransactionType.RESERVE, expireTime);

        if (expiredReservations.isEmpty()) {
            log.info("만료된 예약 없음");
            return;
        }

        // orderId 별로 그룹화
        List<Long> expiredOrderIds = expiredReservations.stream()
                .map(ProductTransactionHistory::getOrderId)
                .distinct()
                .collect(Collectors.toList());

        log.info("만료된 예약 발견 - count: {}, orderIds: {}", expiredOrderIds.size(), expiredOrderIds);

        // 각 주문별로 예약 취소
        for (Long orderId : expiredOrderIds) {
            try {
                productService.cancelReservation(orderId);
                log.info("만료 예약 취소 완료 - orderId: {}", orderId);
            } catch (Exception e) {
                log.error("만료 예약 취소 실패 - orderId: {}, error: {}", orderId, e.getMessage(), e);
            }
        }

        log.info("예약 만료 스케줄러 완료 - 처리된 주문 수: {}", expiredOrderIds.size());
    }
}
