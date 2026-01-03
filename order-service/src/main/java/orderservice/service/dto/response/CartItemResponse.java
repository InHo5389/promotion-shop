package orderservice.service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItemResponse {

    private Long productId;
    private String productName;
    private Long productOptionId;
    private Integer quantity;
    private String size;
    private String color;
    private Integer price;
    private Integer totalPrice;
    private Long appliedCouponId;
    private Integer discountAmount;
    private Integer finalPrice;
}
