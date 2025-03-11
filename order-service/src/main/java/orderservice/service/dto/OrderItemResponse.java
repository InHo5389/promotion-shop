package orderservice.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import orderservice.entity.OrderItem;

import java.math.BigDecimal;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemResponse {
    private Long productId;
    private Long productOptionId;
    private String productName;
    private String optionName;
    private int quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
    private Long couponId;
    private BigDecimal discountPrice;
    private BigDecimal discountedTotalPrice;

    public static OrderItemResponse from(OrderItem item) {
        return OrderItemResponse.builder()
                .productId(item.getProductId())
                .productOptionId(item.getProductOptionId())
                .productName(item.getProductName())
                .optionName(item.getOptionName())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .totalPrice(item.getTotalPrice())
                .build();
    }

    public static OrderItemResponse fromWithDiscount(OrderItem item) {
        OrderItemResponse.OrderItemResponseBuilder builder = OrderItemResponse.builder()
                .productId(item.getProductId())
                .productOptionId(item.getProductOptionId())
                .productName(item.getProductName())
                .optionName(item.getOptionName())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .totalPrice(item.getTotalPrice());

        // 쿠폰 적용된 경우 할인 정보 추가
        if (item.getCouponId() != null) {
            builder.couponId(item.getCouponId())
                    .discountPrice(item.getDiscountPrice())
                    .discountedTotalPrice(item.getDiscountedTotalPrice());
        } else {
            builder.discountedTotalPrice(item.getTotalPrice());
        }

        return builder.build();
    }
}

