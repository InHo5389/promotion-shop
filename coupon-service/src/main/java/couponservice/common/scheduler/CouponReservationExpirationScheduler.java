package couponservice.common.scheduler;

import couponservice.entity.CouponTransactionHistory;
import couponservice.repository.CouponTransactionHistoryJpaRepository;
import couponservice.service.v3.CouponService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static couponservice.entity.CouponTransactionHistory.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class CouponReservationExpirationScheduler {

    private final CouponService couponService;
    private final CouponTransactionHistoryJpaRepository couponTransactionHistoryJpaRepository;

    @Scheduled(fixedRate = 600000)
    public void cancelExpiredReservations() {
        log.info("쿠폰 예약 만료 스케줄러 시작");

        LocalDateTime now  = LocalDateTime.now();

        List<CouponTransactionHistory> expiredReservations =
                couponTransactionHistoryJpaRepository.findExpiredReservations(TransactionType.RESERVE, now );

        if (expiredReservations.isEmpty()) {
            log.info("만료된 쿠폰 예약 없음");
            return;
        }

        List<Long> expiredOrderIds = expiredReservations.stream()
                .map(CouponTransactionHistory::getOrderId)
                .distinct()
                .collect(Collectors.toList());

        log.info("만료된 쿠폰 예약 발견 - count: {}, orderIds: {}", expiredOrderIds.size(), expiredOrderIds);

        for (Long orderId : expiredOrderIds) {
            try {
                couponService.cancelReservation(orderId);
                log.info("만료 쿠폰 예약 취소 완료 - orderId: {}", orderId);
            } catch (Exception e) {
                log.error("만료 쿠폰 예약 취소 실패 - orderId: {}, error: {}", orderId, e.getMessage(), e);
            }
        }

        log.info("쿠폰 예약 만료 스케줄러 완료 - 처리된 주문 수: {}", expiredOrderIds.size());
    }
}
