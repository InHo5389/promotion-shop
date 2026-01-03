package pointservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pointservice.entity.PointTransactionHistory;

import java.time.LocalDateTime;
import java.util.List;

import static pointservice.entity.PointTransactionHistory.*;

public interface PointTransactionHistoryJpaRepository extends JpaRepository<PointTransactionHistory, Long> {
    List<PointTransactionHistory> findByOrderIdAndUserIdAndType(Long orderId, Long userId, TransactionType transactionType);
    List<PointTransactionHistory> findByOrderIdAndType(Long orderId, TransactionType type);


    @Query("""
            SELECT h FROM PointTransactionHistory h
            WHERE h.type = :type
            AND h.reservedAt < :expirationThreshold
            AND NOT EXISTS (
                SELECT c FROM PointTransactionHistory c
                WHERE c.orderId = h.orderId
                AND c.userId = h.userId
                AND c.type IN ('CONFIRM_RESERVE', 'CANCEL_RESERVE')
            )
            """)
    List<PointTransactionHistory> findExpiredReservations(
            @Param("type") TransactionType type,
            @Param("expirationThreshold") LocalDateTime expirationThreshold
    );
}
