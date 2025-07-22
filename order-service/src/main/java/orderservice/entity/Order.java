package orderservice.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import orderservice.common.BaseEntity;
import orderservice.common.exception.CustomGlobalException;
import orderservice.common.exception.ErrorType;
import orderservice.service.dto.OrderRequest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Slf4j
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
    private Long timeSaleId;
    private BigDecimal totalAmount;
    private BigDecimal discountAmount;

    @Setter
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
    public boolean hasPointsUsed() {
        return this.getPointAmount() != null;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }

    public void recalculateAmounts() {
        // 쿠폰 할인 총액 계산
        BigDecimal totalDiscountAmount = this.orderItems.stream()
                .filter(OrderItem::hasDiscount)
                .map(OrderItem::getDiscountPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        this.discountAmount = totalDiscountAmount;

        // 실제 상품 지불 금액 계산
        BigDecimal actualItemsAmount = this.orderItems.stream()
                .map(OrderItem::getActualPaymentAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 포인트 할인 적용하여 최종 금액 계산
        BigDecimal pointDiscount = this.pointAmount != null ? this.pointAmount : BigDecimal.ZERO;
        this.finalAmount = actualItemsAmount.subtract(pointDiscount);

        // 음수 방지
        if (this.finalAmount.compareTo(BigDecimal.ZERO) < 0) {
            this.finalAmount = BigDecimal.ZERO;
        }

        log.info("주문 금액 재계산 완료 - 주문ID: {}, 쿠폰할인: {}원, 포인트할인: {}원, 최종금액: {}원",
                this.id, totalDiscountAmount, pointDiscount, this.finalAmount);
    }

    public void applyPointDiscount(BigDecimal pointAmount) {
        if (pointAmount == null || pointAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new CustomGlobalException(ErrorType.NOT_ENOUGH_POINT);
        }

        this.pointAmount = pointAmount;
    }

    public void applyCouponToProduct(Long productId, Long productOptionId, Long couponId, BigDecimal discountAmount) {
        OrderItem targetItem = findOrderItem(productId, productOptionId);
        targetItem.applyCouponDiscount(couponId, discountAmount);
        recalculateAmounts();

        log.info("특정 상품에 쿠폰 적용 - 주문ID: {}, 상품ID: {}, 쿠폰ID: {}",
                this.id, productId, couponId);
    }

    private OrderItem findOrderItem(Long productId, Long productOptionId) {
        return this.orderItems.stream()
                .filter(item -> item.getProductId().equals(productId)
                        && item.getProductOptionId().equals(productOptionId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        String.format("해당 상품을 찾을 수 없습니다 - productId: %d, optionId: %d",
                                productId, productOptionId)));
    }
}

