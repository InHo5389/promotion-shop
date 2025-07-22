package orderservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Entity
@Table(name = "saga_transactions")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SagaTransaction {

    @Id
    @Column(nullable = false, unique = true)
    private String sagaId;

    @Column(nullable = false)
    private Long orderId;

    @Column(nullable = false)
    private Long userId;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "saga_order_items",
            joinColumns = @JoinColumn(name = "saga_id")
    )
    @Builder.Default
    private List<SagaOrderItem> sagaOrderItems = new ArrayList<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "saga_used_coupons",
            joinColumns = @JoinColumn(name = "saga_id")
    )
    @Builder.Default
    private List<Long> usedCouponIds = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SagaStatus status;

    private String currentStep;
    private String errorMessage;

    // 실행된 단계들 추적
    private Boolean stockDecreased;
    private Boolean couponUsed;
    private Boolean pointUsed;
    private Boolean cartCleared;

    // 보상에 필요한 정보
    private Long usedPointAmount;
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

    public void recordErrorMessage(String error) {
        this.errorMessage = error;
    }

    public void markStockDecreased() {
        this.stockDecreased = true;
    }

    public void markCouponUsed(Long couponId) {
        this.couponUsed = true;
        if (!this.usedCouponIds.contains(couponId)) {
            this.usedCouponIds.add(couponId);
        }
    }

    public void markPointUsed(Long pointAmount) {
        this.pointUsed = true;
        this.usedPointAmount = pointAmount;
    }

    public static SagaTransaction createForOrder(String sagaId, Order order) {
        List<SagaOrderItem> sagaItems = order.getOrderItems().stream()
                .map(item -> SagaOrderItem.builder()
                        .productId(item.getProductId())
                        .productOptionId(item.getProductOptionId())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .build())
                .collect(Collectors.toList());

        return SagaTransaction.builder()
                .sagaId(sagaId)
                .orderId(order.getId())
                .userId(order.getUserId())
                .sagaOrderItems(sagaItems)
                .status(SagaStatus.ORDER_CREATED)
                .currentStep("ORDER_CREATED")
                .stockDecreased(false)
                .couponUsed(false)
                .pointUsed(false)
                .build();
    }
}
