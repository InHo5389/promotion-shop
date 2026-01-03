package orderservice.service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartCouponApplyResponse {

    private Long couponId;
    private Integer originalPrice;
    private Integer discountAmount;
    private Integer finalPrice;
}
