package orderservice.service.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemInfo {

    private Long productId;
    private Long productOptionId;
    private Integer quantity;
    private Integer price;
    private Integer totalPrice;
    private Long couponId;
    private Integer couponDiscount;
}
