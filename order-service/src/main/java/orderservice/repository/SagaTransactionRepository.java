package orderservice.repository;

import orderservice.entity.SagaTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SagaTransactionRepository extends JpaRepository<SagaTransaction, Long> {
    Optional<SagaTransaction> findBySagaId(String sagaId);
    Optional<SagaTransaction> findByOrderId(Long orderId);
}
