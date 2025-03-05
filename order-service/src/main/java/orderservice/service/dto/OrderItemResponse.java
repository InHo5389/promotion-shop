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
    private BigDecimal price;
    private BigDecimal totalPrice;

    public static OrderItemResponse from(OrderItem item) {
        return OrderItemResponse.builder()
                .productId(item.getProductId())
                .productOptionId(item.getProductOptionId())
                .productName(item.getProductName())
                .optionName(item.getOptionName())
                .quantity(item.getQuantity())
                .price(item.getPrice())
                .totalPrice(item.getTotalPrice())
                .build();
    }
}

