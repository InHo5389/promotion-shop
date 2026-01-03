package pointservice.common.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pointservice.entity.PointTransactionHistory;
import pointservice.repository.PointTransactionHistoryJpaRepository;
import pointservice.service.v2.PointService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static pointservice.entity.PointTransactionHistory.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class PointReservationExpirationScheduler {

    private final PointService pointService;
    private final PointTransactionHistoryJpaRepository pointTransactionHistoryJpaRepository;

    @Scheduled(fixedRate = 600000)
    public void cancelExpiredReservations() {
        log.info("포인트 예약 만료 스케줄러 시작");

        LocalDateTime expireTime = LocalDateTime.now().minusMinutes(10);

        List<PointTransactionHistory> expiredReservations =
                pointTransactionHistoryJpaRepository.findExpiredReservations(TransactionType.RESERVE, expireTime);

        if (expiredReservations.isEmpty()) {
            log.info("만료된 포인트 예약 없음");
            return;
        }

        // orderId + userId로 그룹화
        Map<String, PointTransactionHistory> groupedReservations = expiredReservations.stream()
                .collect(Collectors.toMap(
                        h -> h.getOrderId() + "_" + h.getUserId(),
                        h -> h,
                        (a, b) -> a
                ));

        log.info("만료된 포인트 예약 발견 - count: {}", groupedReservations.size());

        for (PointTransactionHistory history : groupedReservations.values()) {
            try {
                pointService.cancelReservation(history.getOrderId(), history.getUserId());
                log.info("만료 포인트 예약 취소 완료 - orderId: {}, userId: {}",
                        history.getOrderId(), history.getUserId());
            } catch (Exception e) {
                log.error("만료 포인트 예약 취소 실패 - orderId: {}, userId: {}, error: {}",
                        history.getOrderId(), history.getUserId(), e.getMessage(), e);
            }
        }

        log.info("포인트 예약 만료 스케줄러 완료 - 처리된 예약 수: {}", groupedReservations.size());
    }
}
