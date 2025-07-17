package orderservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "saga_transactions")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SagaTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String sagaId;

    @Column(nullable = false)
    private Long orderId;

    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SagaStatus status;

    private String currentStep;

    private String lastError;

    // 실행된 단계들 추적
    private Boolean stockDecreased;
    private Boolean couponUsed;
    private Boolean pointUsed;

    // 보상에 필요한 정보
    private Long usedCouponId;
    private Long usedPointAmount;
    private Long productId;
    private Long productOptionId;
    private Integer quantity;
    private LocalDateTime completedAt;

    public void updateStatus(SagaStatus newStatus) {
        this.status = newStatus;
        if (newStatus == SagaStatus.COMPLETED ||
                newStatus == SagaStatus.FAILED ||
                newStatus == SagaStatus.COMPENSATION_COMPLETED) {
            this.completedAt = LocalDateTime.now();
        }
    }

    public void updateStep(String step) {
        this.currentStep = step;
    }

    public void recordError(String error) {
        this.lastError = error;
    }

    public void markStockDecreased() {
        this.stockDecreased = true;
    }

    public void markCouponUsed(Long couponId) {
        this.couponUsed = true;
        this.usedCouponId = couponId;
    }

    public void markPointUsed(Long pointAmount) {
        this.pointUsed = true;
        this.usedPointAmount = pointAmount;
    }

    public static SagaTransaction create(String sagaId, Long orderId, Long userId,
                                              Long productId, Long productOptionId, Integer quantity) {
        return SagaTransaction.builder()
                .sagaId(sagaId)
                .orderId(orderId)
                .userId(userId)
                .status(SagaStatus.STARTED)
                .currentStep("ORDER_CREATION")
                .stockDecreased(false)
                .couponUsed(false)
                .pointUsed(false)
                .productId(productId)
                .productOptionId(productOptionId)
                .quantity(quantity)
                .build();
    }
}
