package orderservice.entity;

import jakarta.persistence.*;
import lombok.*;
import orderservice.common.BaseEntity;
import orderservice.service.dto.OrderRequest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@Table(name = "orders")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private BigDecimal totalAmount;
    private BigDecimal discountAmount;
    private BigDecimal pointAmount;
    private BigDecimal finalAmount;

    @Setter
    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    @Builder.Default
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> orderItems = new ArrayList<>();

    private String address;
    private String receiverName;
    private String receiverPhone;

    private String paymentMethod;
    private String paymentStatus;

    public static Order create(OrderRequest request) {
        return Order.builder()
                .userId(request.getUserId())
                .status(OrderStatus.PENDING)
                .address(request.getAddress())
                .receiverName(request.getReceiverName())
                .receiverPhone(request.getReceiverPhone())
                .paymentMethod(request.getPaymentMethod())
                .paymentStatus("WAITING")
                .build();
    }

    public void addItem(OrderItem orderItem) {
        this.orderItems.add(orderItem);
        orderItem.setOrder(this);
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    // 쿠폰 할인 적용
    public void applyCouponDiscount(BigDecimal discount) {
        this.discountAmount = discount;
        calculateFinalAmount();
    }

    // 포인트 사용 금액 적용
    public void applyPointDiscount(Long pointAmount) {
        this.pointAmount = new BigDecimal(pointAmount);
        calculateFinalAmount();
    }

    // 최종 결제 금액 계산
    private void calculateFinalAmount() {
        BigDecimal afterCoupon = totalAmount.subtract(discountAmount != null ? discountAmount : BigDecimal.ZERO);
        this.finalAmount = afterCoupon.subtract(pointAmount != null ? pointAmount : BigDecimal.ZERO);

        // 최종 금액이 0보다 작으면 0으로 설정
        if (this.finalAmount.compareTo(BigDecimal.ZERO) < 0) {
            this.finalAmount = BigDecimal.ZERO;
        }
    }

    // 쿠폰 할인 후 금액 (포인트 차감 전)
    public BigDecimal getTotalAmountAfterDiscount() {
        BigDecimal afterDiscount = totalAmount.subtract(discountAmount != null ? discountAmount : BigDecimal.ZERO);
        return afterDiscount.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : afterDiscount;
    }

    public boolean isOwnedBy(Long userId){
        return this.userId.equals(userId);
    }

    public boolean isCancelled(){
        return this.status == OrderStatus.CANCELLED;
    }

    public boolean isShippedOrDeliveredOrRefunded() {
        return this.getStatus() == OrderStatus.SHIPPED
                || this.getStatus() == OrderStatus.DELIVERED
                || this.getStatus() == OrderStatus.REFUNDED;
    }

    public boolean hasPointsUsed() {
        return this.getPointAmount() != null;
    }

    public void setFinalAmount(BigDecimal finalAmount) {
        this.finalAmount = finalAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }
}
