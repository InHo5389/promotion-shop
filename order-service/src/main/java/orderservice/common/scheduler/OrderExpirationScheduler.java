package orderservice.common.scheduler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import orderservice.entity.Order;
import orderservice.repository.OrderJpaRepository;
import orderservice.service.v1.OrderService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderExpirationScheduler {

    private final OrderJpaRepository orderRepository;
    private final OrderService orderService;

    @Scheduled(fixedDelay = 60000) // 1분
//    @SchedulerLock(
//            name = "OrderExpirationScheduler_cancelExpiredOrders",
//            lockAtMostFor = "50s",
//            lockAtLeastFor = "10s"
//    )
    public void cancelExpiredOrders() {
        log.info("===== 만료 주문 취소 배치 시작 =====");

        try {
            LocalDateTime now = LocalDateTime.now();
            List<Order> expiredOrders = orderRepository.findExpiredOrders(now);

            if (expiredOrders.isEmpty()) {
                log.info("만료된 주문 없음");
                return;
            }

            log.info("만료된 주문 발견 - count: {}", expiredOrders.size());

            for (Order order : expiredOrders) {
                try {
                    orderService.cancelExpiredOrder(order.getId());
                    log.info("만료 주문 취소 성공 - orderId: {}", order.getId());
                } catch (Exception e) {
                    log.error("만료 주문 취소 실패 - orderId: {}", order.getId(), e);
                }
            }

        } catch (Exception e) {
            log.error("만료 주문 취소 배치 실패", e);
        }
    }
}
