package orderservice.service.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductCouponInfo {

    @NotNull(message = "상품 ID는 필수입니다")
    private Long productId;

    @NotNull(message = "상품 옵션 ID는 필수입니다")
    private Long productOptionId;

    private Long couponId;

    @NotNull(message = "수량은 필수입니다")
    @Min(value = 1, message = "수량은 1개 이상이어야 합니다")
    private Integer quantity;

    @NotNull(message = "상품 단가는 필수입니다")
    @DecimalMin(value = "0.0", message = "상품 단가는 0 이상이어야 합니다")
    private BigDecimal unitPrice;

    @NotNull(message = "장바구니 아이템 ID는 필수입니다")
    private Long cartItemId;
}
