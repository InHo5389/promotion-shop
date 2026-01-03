package couponservice.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@NoArgsConstructor
@Table(name = "coupon_transaction_histories")
public class CouponTransactionHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long orderId;

    @Column(nullable = false)
    private Long couponId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long productOptionId;

    @Column(nullable = false)
    private Integer discountAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    private LocalDateTime reservedAt;

    @Builder
    public CouponTransactionHistory(Long orderId, Long couponId, Long userId, Long productOptionId, Integer discountAmount, TransactionType type, LocalDateTime reservedAt) {
        this.orderId = orderId;
        this.couponId = couponId;
        this.userId = userId;
        this.productOptionId = productOptionId;
        this.discountAmount = discountAmount;
        this.type = type;
        this.reservedAt = reservedAt;
    }

    public static CouponTransactionHistory create(
            Long orderId,
            Long couponId,
            Long userId,
            Long productOptionId,
            Integer discountAmount,
            TransactionType type
    ) {
        return CouponTransactionHistory.builder()
                .orderId(orderId)
                .couponId(couponId)
                .userId(userId)
                .productOptionId(productOptionId)
                .discountAmount(discountAmount)
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

