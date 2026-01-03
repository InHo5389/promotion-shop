package pointservice.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@NoArgsConstructor
@Table(name = "point_transaction_histories")
public class PointTransactionHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long orderId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    private LocalDateTime reservedAt;

    @Builder
    public PointTransactionHistory(Long orderId, Long userId, Long amount, TransactionType type, LocalDateTime reservedAt) {
        this.orderId = orderId;
        this.userId = userId;
        this.amount = amount;
        this.type = type;
        this.reservedAt = reservedAt;
    }

    public static PointTransactionHistory create(
            Long orderId,
            Long userId,
            Long amount,
            TransactionType type
    ) {
        return PointTransactionHistory.builder()
                .orderId(orderId)
                .userId(userId)
                .amount(amount)
                .type(type)
                .reservedAt(LocalDateTime.now())
                .build();
    }

    public enum TransactionType {
        RESERVE,
        CONFIRM_RESERVE,
        CANCEL_RESERVE,
        ROLLBACK_RESERVE,
        ROLLBACK_CONFIRM
    }
}
