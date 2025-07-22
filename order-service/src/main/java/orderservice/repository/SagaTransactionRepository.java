package orderservice.repository;

import orderservice.entity.SagaTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SagaTransactionRepository extends JpaRepository<SagaTransaction, Long> {
    Optional<SagaTransaction> findBySagaId(String sagaId);
    Optional<SagaTransaction> findByOrderId(Long orderId);

    @Query("SELECT s FROM SagaTransaction s " +
            "LEFT JOIN FETCH s.sagaOrderItems " +
            "WHERE s.sagaId = :sagaId")
    Optional<SagaTransaction> findBySagaIdWithOrderItems(@Param("sagaId") String sagaId);

    @Query("SELECT s FROM SagaTransaction s " +
            "LEFT JOIN FETCH s.usedCouponIds " +
            "WHERE s.sagaId = :sagaId")
    Optional<SagaTransaction> findBySagaIdWithCouponIds(@Param("sagaId") String sagaId);
}
