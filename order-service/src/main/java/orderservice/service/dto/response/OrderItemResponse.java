package orderservice.service.dto.response;

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
    private Integer totalPrice;
    private Long couponId;
    private BigDecimal discountPrice;
    private BigDecimal discountedTotalPrice;

    public static OrderItemResponse from(OrderItem item) {
        return OrderItemResponse.builder()
                .productId(item.getProductId())
                .productOptionId(item.getProductOptionId())
                .quantity(item.getQuantity())
                .totalPrice(item.getTotalPrice())
                .build();
    }
}

