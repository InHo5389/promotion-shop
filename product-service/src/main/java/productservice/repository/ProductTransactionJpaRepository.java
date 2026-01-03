package productservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import productservice.entity.ProductTransactionHistory;

import java.time.LocalDateTime;
import java.util.List;

import static productservice.entity.ProductTransactionHistory.*;

public interface ProductTransactionJpaRepository extends JpaRepository<ProductTransactionHistory, Long> {
    List<ProductTransactionHistory> findByOrderIdAndType(Long orderId, TransactionType transactionType);

    @Query("""
            SELECT h FROM ProductTransactionHistory h
            WHERE h.type = :type
            AND h.reservedAt < :expirationThreshold
            AND NOT EXISTS (
                SELECT c FROM ProductTransactionHistory c
                WHERE c.orderId = h.orderId
                AND c.type IN ('CONFIRM_RESERVE', 'CANCEL_RESERVE')
            )
            """)
    List<ProductTransactionHistory> findExpiredReservations(
            @Param("type") TransactionType type,
            @Param("expirationThreshold") LocalDateTime expirationThreshold
    );

    boolean existsByOrderIdAndType(Long orderId, TransactionType type);
}
