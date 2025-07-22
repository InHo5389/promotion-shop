package orderservice.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import orderservice.client.dto.ProductResponse;
import orderservice.common.exception.CustomGlobalException;
import orderservice.common.exception.ErrorType;
import orderservice.service.dto.OrderItemRequest;

import java.math.BigDecimal;

@Slf4j
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

    public void applyCouponDiscount(Long couponId, BigDecimal discountPrice) {

        this.couponId = couponId;
        this.discountPrice = discountPrice;

        BigDecimal discountedPrice = this.totalPrice.subtract(discountPrice);
        this.discountedTotalPrice = discountedPrice.compareTo(BigDecimal.ZERO) < 0
                ? BigDecimal.ZERO : discountedPrice;

        log.info("쿠폰 할인 적용 - 상품ID: {}, 쿠폰ID: {}, 할인금액: {}원, 할인후가격: {}원",
                this.productId, couponId, discountPrice, this.discountedTotalPrice);
    }

    public BigDecimal getActualPaymentAmount() {
        return this.discountedTotalPrice != null ? this.discountedTotalPrice : this.totalPrice;
    }

    public boolean hasDiscount() {
        return this.couponId != null && this.discountPrice != null
                && this.discountPrice.compareTo(BigDecimal.ZERO) > 0;
    }
}
