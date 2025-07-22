package orderservice.service.v1;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import orderservice.entity.Order;
import orderservice.entity.SagaStatus;
import orderservice.entity.SagaTransaction;
import orderservice.repository.SagaTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SagaTransactionService {

    private final SagaTransactionRepository sagaTransactionRepository;

    /**
     * SAGA 생성 - 별도 트랜잭션으로 즉시 커밋
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SagaTransaction createSaga(String sagaId, Order order) {
        log.info("Creating SAGA - sagaId: {}, orderId: {}", sagaId, order.getId());

        SagaTransaction saga = SagaTransaction.createForOrder(sagaId, order);
        SagaTransaction saved = sagaTransactionRepository.save(saga);
        log.info("SAGA created - sagaId: {}", sagaId);

        return saved;
    }

    /**
     * 재고 차감 성공 - 별도 트랜잭션으로 즉시 커밋
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markStockDecreased(String sagaId) {
        log.info("Marking stock decreased - sagaId: {}", sagaId);

        SagaTransaction saga = findBySagaId(sagaId);
        saga.markStockDecreased();
        saga.updateStatus(SagaStatus.STOCK_DECREASED);
        saga.updateStep("STOCK_DECREASED");

        sagaTransactionRepository.save(saga);
        log.info("Stock decreased status committed - sagaId: {}", sagaId);
    }

    /**
     * 재고 차감 실패 - 별도 트랜잭션으로 즉시 커밋
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markStockDecreaseFailed(String sagaId, String errorMessage) {
        log.info("Marking stock decrease failed - sagaId: {}", sagaId);

        SagaTransaction saga = findBySagaId(sagaId);
        saga.updateStatus(SagaStatus.STOCK_DECREASE_FAILED);
        saga.recordErrorMessage(errorMessage);
        saga.updateStep("STOCK_DECREASED_FAILED");

        sagaTransactionRepository.save(saga);
        log.info("Stock decrease failed status committed - sagaId: {}", sagaId);
    }

    /**
     * 쿠폰 사용 성공 - 별도 트랜잭션으로 즉시 커밋
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markCouponUsed(String sagaId, Long couponId) {
        log.info("Marking coupon used - sagaId: {}, couponId: {}", sagaId, couponId);

        SagaTransaction saga = findBySagaId(sagaId);
        saga.markCouponUsed(couponId);
        saga.updateStatus(SagaStatus.COUPON_USED);
        saga.updateStep("COUPON_USED");

        sagaTransactionRepository.save(saga);
        log.info("Coupon used status committed - sagaId: {}", sagaId);
    }

    /**
     * 쿠폰 사용 실패 - 별도 트랜잭션으로 즉시 커밋
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markCouponUseFailed(String sagaId, String errorMessage) {
        log.info("Marking coupon use failed - sagaId: {}", sagaId);

        SagaTransaction saga = findBySagaId(sagaId);
        saga.updateStatus(SagaStatus.COUPON_USE_FAILED);
        saga.recordErrorMessage(errorMessage);
        saga.updateStep("COUPON_USE_FAILED");

        sagaTransactionRepository.save(saga);
        log.info("Coupon use failed status committed - sagaId: {}", sagaId);
    }

    /**
     * 포인트 사용 성공 - 별도 트랜잭션으로 즉시 커밋
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markPointUsed(String sagaId, Long pointAmount) {
        log.info("Marking point used - sagaId: {}, amount: {}P", sagaId, pointAmount);

        SagaTransaction saga = findBySagaId(sagaId);
        saga.markPointUsed(pointAmount);
        saga.updateStatus(SagaStatus.POINT_USED);
        saga.updateStep("POINT_USED");

        sagaTransactionRepository.save(saga);
        log.info("Point used status committed - sagaId: {}", sagaId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markPointUseFailed(String sagaId, String errorMessage) {
        log.info("Marking point use failed - sagaId: {}", sagaId);

        SagaTransaction saga = findBySagaId(sagaId);
        saga.updateStatus(SagaStatus.POINT_USE_FAILED);
        saga.recordErrorMessage(errorMessage);
        saga.updateStep("포인트 사용 실패");

        sagaTransactionRepository.save(saga);
        log.info("Point use failed status committed - sagaId: {}", sagaId);
    }

    /**
     * SAGA 완료 - 별도 트랜잭션으로 즉시 커밋
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markCompleted(String sagaId) {
        log.info("Marking SAGA completed - sagaId: {}", sagaId);

        SagaTransaction saga = findBySagaId(sagaId);
        saga.updateStatus(SagaStatus.COMPLETED);
        saga.updateStep("COMPLETED");

        sagaTransactionRepository.save(saga);
        log.info("SAGA completed status committed - sagaId: {}", sagaId);
    }

    /**
     * SAGA 실패 - 별도 트랜잭션으로 즉시 커밋
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(String sagaId, String errorMessage) {
        log.info("Marking SAGA failed - sagaId: {}", sagaId);

        SagaTransaction saga = findBySagaId(sagaId);
        saga.updateStatus(SagaStatus.FAILED);
        saga.recordErrorMessage(errorMessage);
        saga.updateStep("FAILED");

        sagaTransactionRepository.save(saga);
        log.info("SAGA failed status committed - sagaId: {}", sagaId);
    }

    /**
     * 보상 시작 - 별도 트랜잭션으로 즉시 커밋
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markCompensating(String sagaId) {
        log.info("Marking SAGA compensating - sagaId: {}", sagaId);

        SagaTransaction saga = findBySagaId(sagaId);
        saga.updateStatus(SagaStatus.COMPENSATING);
        saga.updateStep("COMPENSATING");

        sagaTransactionRepository.save(saga);
        log.info("SAGA compensating status committed - sagaId: {}", sagaId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markCompensationCompleted(String sagaId) {
        log.info("Marking SAGA compensation completed - sagaId: {}", sagaId);

        SagaTransaction saga = findBySagaId(sagaId);
        saga.updateStatus(SagaStatus.COMPENSATION_COMPLETED);
        saga.updateStep("보상 완료");

        sagaTransactionRepository.save(saga);
        log.info("SAGA compensation completed status committed - sagaId: {}", sagaId);
    }

    /**
     * DB에서 최신 SAGA 상태 조회
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public SagaTransaction getLatestSaga(String sagaId) {
        return findBySagaId(sagaId);
    }

    private SagaTransaction findBySagaId(String sagaId) {
        return sagaTransactionRepository.findBySagaId(sagaId)
                .orElseThrow(() -> new RuntimeException("SAGA not found: " + sagaId));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public SagaTransaction getLatestSagaWithCollections(String sagaId) {
        SagaTransaction saga = sagaTransactionRepository.findBySagaIdWithOrderItems(sagaId)
                .orElseThrow(() -> new RuntimeException("SAGA not found: " + sagaId));

        sagaTransactionRepository.findBySagaIdWithCouponIds(sagaId);
        return saga;
    }
}
