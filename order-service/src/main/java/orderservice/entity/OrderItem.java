package orderservice.entity;

import jakarta.persistence.*;
import lombok.*;
import orderservice.client.dto.ProductResponse;
import orderservice.common.exception.CustomGlobalException;
import orderservice.common.exception.ErrorType;
import orderservice.service.dto.OrderItemRequest;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "order_items")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    private Order order;

    private Long productId;
    private Long productOptionId;
    private String productName;
    private String optionName;
    private int quantity;

    private Long couponId;
    private BigDecimal discountPrice;

    private BigDecimal unitPrice;
    private BigDecimal discountedTotalPrice;
    private BigDecimal totalPrice;

    public static OrderItem create(OrderItemRequest itemRequest, ProductResponse product) {
        ProductResponse.ProductOptionDTO option = product.getOptions().stream()
                .filter(opt -> opt.getId().equals(itemRequest.getProductOptionId()))
                .findFirst()
                .orElseThrow(() -> new CustomGlobalException(ErrorType.OPTION_NOT_FOUND));

        return OrderItem.builder()
                .productId(itemRequest.getProductId())
                .productOptionId(itemRequest.getProductOptionId())
                .productName(product.getName())
                .optionName(String.format("%s / %s", option.getSize(), option.getColor()))
                .quantity(itemRequest.getQuantity())
                .unitPrice(product.getPrice())
                .totalPrice(product.getPrice().multiply(BigDecimal.valueOf(itemRequest.getQuantity())))
                .build();
    }

    // 쿠폰 할인 적용 메서드
    public void applyCouponDiscount(Long couponId, BigDecimal discountAmount) {
        this.couponId = couponId;
        this.discountPrice = discountAmount;
        calculateDiscountedPrice();
    }

    // 할인된 가격 계산 메서드
    private void calculateDiscountedPrice() {
        // 할인 금액이 없으면 원래 가격 사용
        if (discountPrice == null || discountPrice.compareTo(BigDecimal.ZERO) <= 0) {
            this.discountedTotalPrice = this.totalPrice;
            return;
        }

        // 할인 적용 후 가격 계산
        BigDecimal afterDiscount = this.totalPrice.subtract(this.discountPrice);

        // 음수가 되지 않도록 보장
        if (afterDiscount.compareTo(BigDecimal.ZERO) < 0) {
            afterDiscount = BigDecimal.ZERO;
        }

        this.discountedTotalPrice = afterDiscount;
    }
}
