package productservice.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@NoArgsConstructor
@Table(name = "stock_transaction_histories")
public class ProductTransactionHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long orderId;

    @Column(nullable = false)
    private Long productOptionId;

    @Column(nullable = false)
    private Integer quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    private LocalDateTime reservedAt;


    @Builder
    public ProductTransactionHistory(Long orderId, Long productOptionId, Integer quantity, TransactionType type, LocalDateTime reservedAt) {
        this.orderId = orderId;
        this.productOptionId = productOptionId;
        this.quantity = quantity;
        this.type = type;
        this.reservedAt = reservedAt;
    }

    public static ProductTransactionHistory create(Long orderId, Long productOptionId, Integer quantity, TransactionType type) {
        return ProductTransactionHistory.builder()
                .orderId(orderId)
                .productOptionId(productOptionId)
                .quantity(quantity)
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
