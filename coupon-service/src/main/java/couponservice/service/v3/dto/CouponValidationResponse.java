package couponservice.service.v3.dto;

import couponservice.entity.CouponPolicy;
import couponservice.entity.DiscountType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponValidationResponse {
    private boolean valid;
    private String invalidReason;
    private CouponPolicyDto couponPolicy;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CouponPolicyDto {
        private String  discountType;
        private Integer discountValue;
        private Integer minimumOrderAmount;
        private Integer maximumDiscountAmount;

        public static CouponPolicyDto from(CouponPolicy policy) {
            return CouponPolicyDto.builder()
                    .discountType(policy.getDiscountType().name())
                    .discountValue(policy.getDiscountValue())
                    .minimumOrderAmount(policy.getMinimumOrderAmount())
                    .maximumDiscountAmount(policy.getMaximumDiscountAmount())
                    .build();
        }
    }
}