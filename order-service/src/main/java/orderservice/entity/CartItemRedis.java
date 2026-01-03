package orderservice.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class CartItemRedis implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long productId;
    private Long productOptionId;
    private Integer quantity;
    private Long appliedCouponId;
    private Integer couponDiscount;
    private LocalDateTime addedAt;

    public String getFieldKey() {
        return productId + "::" + productOptionId;
    }

    public static CartItemRedis create(Long productId, Long productOptionId, Integer quantity) {
        return CartItemRedis.builder()
                .productId(productId)
                .productOptionId(productOptionId)
                .quantity(quantity)
                .appliedCouponId(null)
                .couponDiscount(0)
                .addedAt(LocalDateTime.now())
                .build();
    }

    public void applyCoupon(Long couponId, Integer discount) {
        this.appliedCouponId = couponId;
        this.couponDiscount = discount != null ? discount : 0;
    }

    public void removeCoupon() {
        this.appliedCouponId = null;
        this.couponDiscount = 0;
    }
}
