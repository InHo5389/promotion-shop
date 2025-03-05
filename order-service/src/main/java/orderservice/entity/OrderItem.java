package orderservice.entity;

import jakarta.persistence.*;
import lombok.*;
import orderservice.client.dto.ProductResponse;
import orderservice.common.exception.CustomGlobalException;
import orderservice.common.exception.ErrorType;
import orderservice.service.dto.OrderItemRequest;

import java.math.BigDecimal;

@Getter
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
    private BigDecimal price;
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
                .price(product.getPrice())
                .totalPrice(product.getPrice().multiply(BigDecimal.valueOf(itemRequest.getQuantity())))
                .build();
    }
}
