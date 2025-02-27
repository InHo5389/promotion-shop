package userservice.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import userservice.client.dto.ProductResponse;

import java.math.BigDecimal;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartProductResponse {
    private Long productId;
    private String productName;
    private String image;
    private String status;
    private Long optionId;
    private String optionName;
    private Integer quantity;
    private BigDecimal price;
    private BigDecimal totalPrice;

    public static CartProductResponse from(CartItemRedis item, ProductResponse product, ProductResponse.ProductOptionDto option) {
        BigDecimal price = option.getAdditionalPrice() != null ?
                product.getPrice().add(option.getAdditionalPrice()) :
                product.getPrice();

        return CartProductResponse.builder()
                .productId(item.getProductId())
                .productName(product.getName())
                .image(product.getImage())
                .status(product.getStatus())
                .optionId(item.getOptionId())
                .optionName(option.getSize() + "/" + option.getColor())
                .quantity(item.getQuantity())
                .price(price)
                .totalPrice(price.multiply(new BigDecimal(item.getQuantity())))
                .build();
    }
}