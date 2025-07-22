package orderservice.service.v1;

import event.EventType;
import event.payload.compensation.CouponCompensationRequestPayload;
import event.payload.compensation.PointCompensationRequestPayload;
import event.payload.compensation.StockCompensationRequestPayload;
import event.publisher.EventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import orderservice.entity.SagaOrderItem;
import orderservice.entity.SagaTransaction;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class CompensationService {

    private final SagaTransactionService sagaTransactionService;
    private final EventPublisher eventPublisher;

    /**
     * 보상 트랜잭션 실행 - DB에서 최신 SAGA 상태 조회
     */
    public void executeCompensation(String sagaId) {
        log.info("Starting compensation - sagaId: {}", sagaId);

        // 보상 시작 상태 저장 (별도 트랜잭션)
        sagaTransactionService.markCompensating(sagaId);

        try {
            // DB에서 최신 SAGA 상태 조회
            SagaTransaction saga = sagaTransactionService.getLatestSagaWithCollections(sagaId);

            log.info("Retrieved SAGA for compensation - sagaId: {}, stockDecreased: {}, couponUsed: {}, pointUsed: {}",
                    sagaId, saga.getStockDecreased(), saga.getCouponUsed(), saga.getPointUsed());

            // 포인트 사용 보상 (역순으로)
            if (saga.getPointUsed() && saga.getUsedPointAmount() != null) {
                publishPointCompensation(saga);
            }

            // 쿠폰 사용 보상
            if (saga.getCouponUsed() && !saga.getUsedCouponIds().isEmpty()) {
                publishCouponCompensation(saga);
            }

            // 재고 차감 보상
            if (saga.getStockDecreased()) {
                publishStockCompensation(saga);
            }

            log.info("Compensation events published - sagaId: {}", sagaId);

        } catch (Exception e) {
            log.error("Failed to publish compensation events - sagaId: {}, error: {}",
                    sagaId, e.getMessage(), e);
        }
    }

    private void publishStockCompensation(SagaTransaction saga) {
        for (SagaOrderItem item : saga.getSagaOrderItems()) {
            StockCompensationRequestPayload payload = StockCompensationRequestPayload.builder()
                    .sagaId(saga.getSagaId())
                    .orderId(saga.getOrderId())
                    .userId(saga.getUserId())
                    .productId(item.getProductId())
                    .productOptionId(item.getProductOptionId())
                    .quantity(item.getQuantity())
                    .timestamp(System.currentTimeMillis())
                    .build();

            eventPublisher.publishEvent(
                    EventType.STOCK_COMPENSATION_REQUEST,
                    payload,
                    EventType.Topic.STOCK_COMPENSATION_REQUEST
            );
            log.info("Stock compensation event published - sagaId: {}", saga.getSagaId());
        }
    }

    private void publishCouponCompensation(SagaTransaction saga) {
        for (Long couponId : saga.getUsedCouponIds()) {
            CouponCompensationRequestPayload payload = CouponCompensationRequestPayload.builder()
                    .sagaId(saga.getSagaId())
                    .orderId(saga.getOrderId())
                    .userId(saga.getUserId())
                    .couponId(couponId)
                    .timestamp(System.currentTimeMillis())
                    .build();

            eventPublisher.publishEvent(
                    EventType.COUPON_COMPENSATION_REQUEST,
                    payload,
                    EventType.Topic.COUPON_COMPENSATION_REQUEST
            );

            log.info("Coupon compensation event published - sagaId: {}, couponId: {}",
                    saga.getSagaId(), couponId);
        }
    }

    private void publishPointCompensation(SagaTransaction saga) {
        PointCompensationRequestPayload payload = PointCompensationRequestPayload.builder()
                .sagaId(saga.getSagaId())
                .orderId(saga.getOrderId())
                .userId(saga.getUserId())
                .pointAmount(saga.getUsedPointAmount())
                .timestamp(System.currentTimeMillis())
                .build();

        eventPublisher.publishEvent(
                EventType.POINT_COMPENSATION_REQUEST,
                payload,
                EventType.Topic.POINT_COMPENSATION_REQUEST
        );

        log.info("Point compensation event published - sagaId: {}", saga.getSagaId());
    }
}
