package couponservice.repository;

import couponservice.entity.CouponStatus;
import couponservice.entity.CouponTransactionHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

import static couponservice.entity.CouponTransactionHistory.*;

public interface CouponTransactionHistoryJpaRepository extends JpaRepository<CouponTransactionHistory, Long> {
    List<CouponTransactionHistory> findByOrderIdAndType(Long orderId, TransactionType type);

    @Query("""
            SELECT h FROM CouponTransactionHistory h
            WHERE h.type = :type
            AND h.reservedAt < :expirationThreshold
            AND NOT EXISTS (
                SELECT c FROM CouponTransactionHistory c
                WHERE c.orderId = h.orderId
                AND c.userId = h.userId
                AND c.type IN ('CONFIRM_RESERVE', 'CANCEL_RESERVE')
            )
            """)
    List<CouponTransactionHistory> findExpiredReservations(
            @Param("type") TransactionType type,
            @Param("expirationThreshold") LocalDateTime expirationThreshold
    );
}
